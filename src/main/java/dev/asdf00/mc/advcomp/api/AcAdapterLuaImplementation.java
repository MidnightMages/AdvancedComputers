package dev.asdf00.mc.advcomp.api;

import net.minecraft.world.level.block.Block;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AcAdapterLuaImplementation {
    /**
     * The block class that this adapter is built for. Block.class is used as a placeholder for purely abstract classes.
     */
    Class<? extends Block> block() default Block.class;

    Class<?>[] inheritsFrom() default {};

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Method {
        boolean isAbstract() default false;

        String name() default "";
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PropertyGet {
        boolean isAbstract() default false;

        String name() default "";
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PropertySet {
        boolean isAbstract() default false;

        String name() default "";
    }
}
