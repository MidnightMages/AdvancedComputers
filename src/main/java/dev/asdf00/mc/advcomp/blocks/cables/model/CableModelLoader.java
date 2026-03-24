package dev.asdf00.mc.advcomp.blocks.cables.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import java.util.function.Function;

// a lot of stuff taken from https://www.mcjty.eu/docs/1.20/ep5; Thank you :)
public class CableModelLoader implements IGeometryLoader<CableModelLoader.CableModelGeometry> {
    public static final ResourceLocation GENERATOR_LOADER = new ResourceLocation(AdvancedComputers.MODID, "cableloader");

    public static void register(ModelEvent.RegisterGeometryLoaders event) {
        event.register("cableloader", new CableModelLoader());
    }


    @Override
    public CableModelGeometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
//        boolean facade = jsonObject.has("facade") && jsonObject.get("facade").getAsBoolean();
        String cableVariant = jsonObject.get("cableVariant").getAsString();
        return new CableModelGeometry(cableVariant);
    }

    public static class CableModelGeometry implements IUnbakedGeometry<CableModelGeometry> {
        private final String cableVariant;

        public CableModelGeometry(String cableVariant) {
            this.cableVariant = cableVariant;
        }

        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {
            return new CableBakedModel(context, cableVariant);
        }
    }
}
