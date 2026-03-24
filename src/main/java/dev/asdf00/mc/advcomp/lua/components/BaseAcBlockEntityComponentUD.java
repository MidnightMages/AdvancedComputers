package dev.asdf00.mc.advcomp.lua.components;

import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.utils.LuaSerializationUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.commons.lang3.function.TriFunction;

import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class BaseAcBlockEntityComponentUD<BE extends BlockEntity> extends BaseAcComponent {
    protected final BE blockEntity;

    protected BaseAcBlockEntityComponentUD(String componentType, BE blockEntity) {
        super(componentType);
        this.blockEntity = blockEntity;
    }

    protected BaseAcBlockEntityComponentUD(String componentType, LuaVirtualMachine acVm, boolean isAccessible, BE blockEntity) {
        super(componentType, acVm, isAccessible);
        this.blockEntity = blockEntity;
    }

    public BE getBlockEntity() {
        return blockEntity;
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        var byteArrayBuilder = new ByteArrayBuilder(Integer.BYTES * 3 + 1).append(isAccessible);
        return LuaSerializationUtils.appendBlockEntity(byteArrayBuilder, blockEntity).toArray();
    }

    protected static <T extends BaseAcComponent, BE extends BlockEntity> T genericDeserialize(
            Class<BE> blockEntityClazz, TriFunction<LuaVirtualMachine, Boolean, BE, T> constructor,
            LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        boolean isAccessible = reader.readBool();
        BE be = LuaSerializationUtils.readBlockEntity(reader, ((LuaVirtualMachine) additionalData).computerBlockEntity.getLevel());
        if (be == null || !blockEntityClazz.isAssignableFrom(be.getClass())) {
            throw new IllegalStateException("we did not find some " + blockEntityClazz.getSimpleName());
        }
        return constructor.apply((LuaVirtualMachine) additionalData, isAccessible, be);
    }
}
