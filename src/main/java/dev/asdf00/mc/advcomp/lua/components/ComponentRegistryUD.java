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

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ComponentRegistryUD implements LuaUserData {
    private LuaObject luaIdentity;

    private final ArrayList<LuaComponent> allComponents = new ArrayList<>();
    private final LuaVirtualMachine lvm;
    private final Object componentModifyLockObj = new Object();
    private final HashMap<String, LuaUserDataComponent> itemstackAssociationMap = new HashMap<>();

    public ComponentRegistryUD(LuaVirtualMachine lvm) {
        this.lvm = lvm;
    }

    @LuaCallable
    public LuaObject[] list() { // TODO replace with something that can be serialized

        // TODO sort allComponents by inventory-first, then euclidean distance, then by y x and z distances
        // or more generally, sort first by euclidean distance, then by y x z distances, then by slot index

        LuaObject[][] rets;
        synchronized (componentModifyLockObj) {
            rets = allComponents.stream().map(LuaComponent::asLuaObj).toArray(LuaObject[][]::new);
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
            return allComponents.stream().filter(x -> x.type().equals(componentType)).map(LuaComponent::comp).findFirst().orElse(LuaObject.NIL);
        }
    }

    public void addComponentAndNotify(LuaUserDataComponent component, String identifier) {
        addComponentAndNotify(component.getComponentType(), component, identifier);
    }

    public void addComponentAndNotify(String type, LuaUserDataComponent component, String identifier) {
        // trigger compilation of this UD binding ahead of time, so we dont have to wait for it later
        triggerUserdataDescriptorCompilation(component.getClass());
        component.onVmInit(lvm);

        var comp = new LuaComponent(type, LuaObject.of(component));
        synchronized (componentModifyLockObj) {
            if (identifier != null) // built-in components dont have an item stack
                itemstackAssociationMap.put(identifier, component);

            allComponents.add(comp);
        }
        lvm.eventQueue.addComponentAdded(comp);
    }

    public void removeComponentAndNotify(String identifier) {
        synchronized (componentModifyLockObj) {
            var component = itemstackAssociationMap.get(identifier);
            if (component != null) {
                component.makeObjectInaccessible();
                allComponents.removeIf(x -> x.comp.refVal == component);
                lvm.eventQueue.addComponentRemoved(component);
            }
        }
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        var builder = new ByteArrayBuilder();
        var entries = itemstackAssociationMap.entrySet();
        builder.append(entries.size());
        for (var e : entries) {
            builder.append(e.getKey());
            builder.append(LuaObject.of(e.getValue()).serialize(serialData, mappedObjs, additionalData));
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
            keys[i] = reader.readString();
            wrappers[i] = objs[reader.readInt()];
        }
        postActions.add(() -> {
            // this happens AFTER all UD objects have been initialized, now we may unwrap our components
            // TODO do list handling here instead of delegating to addAndNotify
            for (int i = 0; i < compMapLen; i++) {
                nu.addComponentAndNotify((LuaUserDataComponent) wrappers[i].refVal, keys[i]);
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

    public record LuaComponent(String type, LuaObject comp) {
        public LuaObject[] asLuaObj() {
            return new LuaObject[]{LuaObject.of(type), comp};
        }
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