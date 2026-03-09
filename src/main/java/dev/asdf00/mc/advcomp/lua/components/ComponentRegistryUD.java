package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.types.RuntimeAssert;
import dev.asdf00.mc.advcomp.utils.Tuple;
import dev.asdf00.mc.advcomp.utils.TupleArrayListMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ComponentRegistryUD implements LuaUserData {
    private LuaObject luaIdentity;

    private final LuaVirtualMachine lvm;
    private final Object componentModifyLockObj = new Object();
    private final TupleArrayListMap<AcComponentSlotInfo, LuaUserDataComponent> itemstackAssociationMap = new TupleArrayListMap<>();

    public ComponentRegistryUD(LuaVirtualMachine lvm) {
        this.lvm = lvm;
    }

    @LuaCallable
    public LuaObject[] list() { // TODO replace with something that can be serialized

        // TODO sort allComponents by inventory-first, then euclidean distance, then by y x and z distances
        // or more generally, sort first by euclidean distance, then by y x z distances, then by slot index

        LuaObject[][] rets;
        synchronized (componentModifyLockObj) {
            rets = Arrays.stream(itemstackAssociationMap.entries())
                    .map(Tuple::y)
                    .map(x ->
                            new LuaObject[]{LuaObject.of(x.getComponentType()), LuaObject.of(x)}
                    )
                    .toArray(LuaObject[][]::new);
        }
        return new LuaObject[]{
                AtomicLuaFunction.forManyResults(null, (vm, state) -> {
                    var oldIdx = state.get(LuaObject.of(0));
                    if (!oldIdx.isLong()) {
                        vm.error(LuaObject.of("Internal error, or someone messed with the iterator state"));
                        return null;
                    }
                    int nuIdx = (int) oldIdx.asLong() + 1;
                    if (nuIdx < rets.length && nuIdx >= 0) {
                        state.set(LuaObject.of(0), LuaObject.of(nuIdx));
                        return rets[nuIdx];
                    } else {
                        return new LuaObject[0];
                    }
                }).obj(),
                LuaObject.table(LuaObject.of(0), LuaObject.of(-1))
        };
    }

    @LuaCallable
    public LuaObject getFirst(String componentType) {
        synchronized (componentModifyLockObj) {
            return Arrays.stream(itemstackAssociationMap.entries())
                    .map(Tuple::y)
                    .filter(x -> x.getComponentType().equals(componentType))
                    .map(LuaObject::of)
                    .findFirst()
                    .orElse(LuaObject.NIL);
        }
    }

    /**
     * @param component
     * @param sourceInfo
     */
    public void addComponentInitAndNotify(LuaUserDataComponent component, AcComponentSlotInfo sourceInfo) {
        // trigger compilation of this UD binding ahead of time, so we dont have to wait for it later
        triggerUserdataDescriptorCompilation(component.getClass());
        var slotId = sourceInfo.getSlotIndex();
        var isComputer = lvm.computerBlockEntity.getBlockPos().equals(sourceInfo.getInventoryOwnerPos());
        RuntimeAssert.RuntimeAssert(isComputer || slotId == -1, "Only computer supported right now as blocks that contain an inventory of components");
        component.onVmInit(lvm, (isComputer && slotId != -1) ? lvm.computerBlockEntity.itemHandler.getStackInSlot(slotId) : null);

        synchronized (componentModifyLockObj) {
            // builtin components are represented using identifier=null
            itemstackAssociationMap.put(isComputer ? null : sourceInfo, component);
        }
        lvm.triggerMachineEvent("componentAdded", LuaObject.of(component.getComponentType()), LuaObject.of(component));
    }

    public void removeComponentAndNotify(AcComponentSlotInfo slotInfo) {
        synchronized (componentModifyLockObj) {
            var component = itemstackAssociationMap.get(slotInfo);
            if (component != null) {
                component.makeObjectInaccessible();
                lvm.triggerMachineEvent("componentRemoved", LuaObject.of(component));
            }
        }
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        var builder = new ByteArrayBuilder();
        TupleArrayListMap.SerializeData<AcComponentSlotInfo, LuaUserDataComponent> lists;
        synchronized (componentModifyLockObj) {
            lists = itemstackAssociationMap.getDataToSerialize();
        }
        builder.append(lists.a().size());
        for (int i = 0; i < lists.a().size(); i++) {
            AcComponentSlotInfo key = lists.a().get(i);

            builder.append(key.getParsableIdentifier());
            builder.append(LuaObject.of(lists.b().get(i)).serialize(serialData, mappedObjs, additionalData));
        }
        return builder.toArray();
    }

    @LuaDeserializer
    public static ComponentRegistryUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var acVM = (LuaVirtualMachine) additionalData;
        final var nu = new ComponentRegistryUD(acVM);
        acVM.onUdDeserialize(nu);
        final int compMapLen = reader.readInt();
        final String[] keys = new String[compMapLen];
        final LuaObject[] wrappers = new LuaObject[compMapLen];
        for (int i = 0; i < compMapLen; i++) {
            var s = reader.readString();
            keys[i] = s.isEmpty() ? null : s;
            wrappers[i] = objs[reader.readInt()];
        }
        postActions.add(() -> {
            // this happens AFTER all UD objects have been initialized, now we may unwrap our components
            synchronized (nu.componentModifyLockObj) {
                for (int i = 0; i < compMapLen; i++) {
                    nu.itemstackAssociationMap.put(AcComponentSlotInfo.parse(keys[i]), (LuaUserDataComponent) wrappers[i].refVal);
                }
            }
        });
        return nu;
    }

    @Override
    public final LuaObject getSelfAsLuaObject() {
        return luaIdentity;
    }

    @Override
    public final void setSelfAsLuaObject(LuaObject self) {
        luaIdentity = self;
    }

    private static final ExecutorService UD_DESCRIPTOR_COMPILATION_POOL = new ThreadPoolExecutor(0, 1,
            3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());

    private static void triggerUserdataDescriptorCompilation(Class<? extends LuaUserData> udType) {
        if (LuaVM_RT.isDescriptorAvailable(udType)) // threadsafety: if this check is true, we _definitely_ already compiled this
            return;

        UD_DESCRIPTOR_COMPILATION_POOL.submit(() -> {
                    try {
                        LuaVM_RT.getDescriptor(udType);
                    } catch (Exception ignored) {
                        // error will be logged implicitly as we will trigger another compilation anyway if we actually use it
                    }
                }
        );
    }

    public void removeAllComponentsInSlot(Function<AcComponentSlotInfo, Boolean> filter) {
        synchronized (componentModifyLockObj) {
            for (var key : Arrays.stream(itemstackAssociationMap.entries()).filter(x -> filter.apply(x.x())).map(Tuple::x).toArray()) {
                RuntimeAssert.RuntimeAssert(key != null, "key was null");
                itemstackAssociationMap.remove((AcComponentSlotInfo) key);
            }
        }
    }
}