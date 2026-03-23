package dev.asdf00.mc.advcomp.utils;

import net.neoforged.common.ForgeConfigSpec;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class AcConfigBuilder extends ForgeConfigSpec.Builder {

    @Override
    public <T> ForgeConfigSpec.ConfigValue<T> define(List<String> path, Supplier<T> defaultSupplier, Predicate<Object> validator, Class<?> clazz) {
        this.comment("[default: %s]".formatted(defaultSupplier.get()));
        return super.define(path, defaultSupplier, validator, clazz);
    }
}
