package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.items.BaseMassStorageUD;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import dev.asdf00.mc.advcomp.utils.Tuple;
import dev.asdf00.mc.advcomp.utils.TupleArrayListMap;

import java.util.*;
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
    public LuaObject list() {
        LuaObject[] rets;
        synchronized (componentModifyLockObj) {
            rets = Arrays.stream(itemstackAssociationMap.entries())
                    .sorted(getComponentComparator())
                    .map(Tuple::y)
                    .map(x ->
                            LuaObject.of(LuaObject.of(x.getComponentType()), LuaObject.of(x)) // create ARRAY
                    )
                    .toArray(LuaObject[]::new);
        }

        return LuaObject.of(LuaVirtualMachine.BUILTIN_FUNCTIONS.getFunction("$internal.unpacking_iterator",
                LuaObject.tableFromArray(rets),
                new LuaObject[]{LuaObject.of(1)}
        ));
    }

    @LuaCallable
    public LuaObject getFirst(String componentType) {
        synchronized (componentModifyLockObj) {
            return Arrays.stream(itemstackAssociationMap.entries())
                    .sorted(getComponentComparator())
                    .map(Tuple::y)
                    .filter(x -> x.getComponentType().equals(componentType))
                    .map(LuaObject::of)
                    .findFirst()
                    .orElse(LuaObject.NIL);
        }
    }

    Map<String, Integer> massStorageSortOrder = Map.of(
            "hdd", 0,
            "floppy", 1
    );

    // sort first by built-in-ness, then euclidean distance, then by y x z distances, then by slot index
    private Comparator<? super Tuple<AcComponentSlotInfo, LuaUserDataComponent>> getComponentComparator() {
        return (a, b) -> { // smallest ones comes first
            var slotA = a.x();
            var slotB = b.x();
            if ((slotA == null) != (slotB == null)) { // if a is a built in component and b is not, rank a it first
                return slotA == null ? -1 : 1;
            }
            // now slotA and slotB are either not null or both null
            if (slotA == null) { // both are built in
                if (a.y() instanceof BaseMassStorageUD aStorage && b.y() instanceof BaseMassStorageUD bStorage) {
                    if (!aStorage.storageApiType.equals(bStorage.storageApiType)) { // rank managed before unmanaged
                        return aStorage.storageApiType.equals("managed") ? -1 : 1;
                    }

                    var aPos = massStorageSortOrder.getOrDefault(aStorage.storageFamilyName, 8192);
                    var bPos = massStorageSortOrder.getOrDefault(bStorage.storageFamilyName, 8192);
                    return aPos - bPos; // if a is less than b, rank it first
                }

                return a.y().getComponentType().compareTo(b.y().getComponentType());
            } // else both slotinfos are nonnull

            var slotAPos = slotA.getInventoryOwnerPos();
            var slotBPos = slotB.getInventoryOwnerPos();
            if (!slotAPos.equals(slotBPos)) { // inventory block positions differ
                // todo compute euclidean distance to computer instead
                return slotA.getSlotIndex() != -1 ? -1 : 1;
            } else {
                return slotA.getSlotIndex() - slotB.getSlotIndex(); // rank first slots first
            }
        };
    }

    @SuppressWarnings("unchecked")
    public <T extends LuaUserData> T getSingleOfType(Class<T> type) {
        synchronized (componentModifyLockObj) {
            var rv = Arrays.stream(itemstackAssociationMap.entries())
                    .map(Tuple::y)
                    .filter(x -> x.getClass() == type).toArray(LuaUserData[]::new);
            if (rv.length != 1)
                throw new RuntimeException("Unable to look up component of type %s. It existed %s times.".formatted(type.toString(), rv.length));

            return (T) rv[0];
        }
    }

    /**
     * @param component
     * @param sourceInfo
     */
    public void addComponentInitAndNotify(LuaUserDataComponent component, AcComponentSlotInfo sourceInfo) {
        // trigger compilation of this UD binding ahead of time, so we dont have to wait for it later
        triggerUserdataDescriptorCompilation(component.getClass());
        component.onVmInit(lvm, (sourceInfo != null && sourceInfo.isItemComponent()) ?
                lvm.computerBlockEntity.itemHandler.getStackInSlot(sourceInfo.getSlotIndex()) : null);
        synchronized (componentModifyLockObj) {
            // builtin components are represented using identifier=null
            itemstackAssociationMap.put(sourceInfo, component);
        }
        lvm.triggerMachineEvent("componentAdded", LuaObject.of(component.getComponentType()), LuaObject.of(component));
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

            builder.append(key == null ? "" : key.getParsableIdentifier());
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
                    nu.itemstackAssociationMap.put(keys[i] == null ? null : AcComponentSlotInfo.parse(keys[i]),
                            (LuaUserDataComponent) wrappers[i].refVal);
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

    private static ExecutorService UD_DESCRIPTOR_COMPILATION_POOL;
    public static void StartThreadPool() {
        UD_DESCRIPTOR_COMPILATION_POOL = new ThreadPoolExecutor(0, 1,
                3, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    }

    public static void StopThreadPool() {
        UD_DESCRIPTOR_COMPILATION_POOL.shutdownNow();
        UD_DESCRIPTOR_COMPILATION_POOL = null;
    }


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

    public void removeAllMatchingComponents(Function<AcComponentSlotInfo, Boolean> filter) {
        synchronized (componentModifyLockObj) {
            for (var key : Arrays.stream(itemstackAssociationMap.entries()).filter(x -> filter.apply(x.x())).map(Tuple::x).toArray()) {
                RuntimeAssert.RuntimeAssert(key != null, "key was null");
                var removedComponent = itemstackAssociationMap.remove((AcComponentSlotInfo) key);
                if (removedComponent != null) {
                    removedComponent.makeObjectInaccessible();
                    AdvancedComputers.LOGGER.warn("component was removed! %s".formatted(removedComponent.getComponentType()));
                    lvm.triggerMachineEvent("componentRemoved", LuaObject.of(removedComponent));
                }
            }
        }
    }
}