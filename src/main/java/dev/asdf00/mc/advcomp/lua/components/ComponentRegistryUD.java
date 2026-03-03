package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
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

public class ComponentRegistryUD implements LuaUserData {
    private LuaObject luaIdentity;

    private final LuaVirtualMachine lvm;
    private final Object componentModifyLockObj = new Object();
    private final TupleArrayListMap<String, LuaUserDataComponent> itemstackAssociationMap = new TupleArrayListMap<>();

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

    public void addComponentAndNotify(LuaUserDataComponent component, String identifier) {
        // trigger compilation of this UD binding ahead of time, so we dont have to wait for it later
        triggerUserdataDescriptorCompilation(component.getClass());
        component.onVmInit(lvm);

        synchronized (componentModifyLockObj) {
                itemstackAssociationMap.put(identifier, component);
        }
        lvm.eventQueue.addComponentAdded(component);
    }

    public void removeComponentAndNotify(String identifier) {
        synchronized (componentModifyLockObj) {
            var component = itemstackAssociationMap.get(identifier);
            if (component != null) {
                component.makeObjectInaccessible();
                lvm.eventQueue.addComponentRemoved(component);
            }
        }
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        var builder = new ByteArrayBuilder();
        var lists = itemstackAssociationMap.getDataToSerialize();
        builder.append(lists.a().size());
        for (int i = 0; i < lists.a().size(); i++) {
            var key = lists.a().get(i);
            RuntimeAssert.RuntimeAssert(!"".equals(key), "key was an empty string. Use null instead!");
            if (key == null)
                key = "";
            builder.append(key);
            builder.append(LuaObject.of(lists.b().get(i)).serialize(serialData, mappedObjs, additionalData));
        }
        return builder.toArray();
    }

    @LuaDeserializer
    public static ComponentRegistryUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        final var nu = new ComponentRegistryUD((LuaVirtualMachine) additionalData);
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
                    nu.itemstackAssociationMap.put(keys[i], (LuaUserDataComponent) wrappers[i].refVal);
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
}