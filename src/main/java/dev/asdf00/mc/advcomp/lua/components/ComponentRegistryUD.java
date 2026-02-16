package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.api.functions.AtomicLuaFunction;
import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.internals.LuaVM_RT;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ComponentRegistryUD implements LuaUserData {
    private final ArrayList<LuaComponent> allComponents = new ArrayList<>();
    private final LuaVirtualMachine lvm;

    public ComponentRegistryUD(LuaVirtualMachine lvm) {
        this.lvm = lvm;
    }

    @LuaCallable
    public LuaObject[] list() { // TODO replace with something that can be serialized

        // TODO sort allComponents by inventory-first, then euclidean distance, then by y x and z distances
        // or more generally, sort first by euclidean distance, then by y x z distances, then by slot index
        var rets = allComponents.stream().map(LuaComponent::asLuaObj).toArray(LuaObject[][]::new);
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
        return allComponents.stream().filter(x -> x.type().equals(componentType)).map(LuaComponent::comp).findFirst().orElse(LuaObject.NIL);
    }

    public void addComponentAndNotify(LuaUserDataComponent component) {
        addComponentAndNotify(component.getComponentType(), component);
    }

    public void addComponentAndNotify(String type, LuaUserData component) {
        // trigger compilation of this UD binding ahead of time, so we dont have to wait for it later
        triggerUserdataDescriptorCompilation(component.getClass());

        var comp = new LuaComponent(type, LuaObject.of(component));
        allComponents.add(comp);
        lvm.eventQueue.addComponentAdded(comp);
    }

    public void removeComponentAndNotify(LuaUserData component) {
        throw new NotImplementedException();
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static ComponentRegistryUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
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