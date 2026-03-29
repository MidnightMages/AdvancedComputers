package dev.asdf00.mc.advcomp.blocks.screen;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.ClientStatics;
import dev.asdf00.mc.advcomp.blocks.computer.ComputerBlock;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.*;

public class ScreenBlockEntityRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
    public ScreenBlockEntityRenderer(BlockEntityRendererProvider.Context ignored) {
    }


    @Override
    public boolean shouldRenderOffScreen(@NotNull ScreenBlockEntity pBlockEntity) {
        return false;
    }


    @Override
    public void render(@NotNull ScreenBlockEntity pBlockEntity, float pPartialTick, @NotNull PoseStack pPoseStack,
                       @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        pPoseStack.pushPose();
        var facing = pBlockEntity.getBlockState().getValue(ComputerBlock.FACING);

        pPoseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack_mulFacing(pPoseStack, facing);
        pPoseStack.translate(-0.5f, -0.5f, -0.5f);

        if (false) {
            var buf = pBuffer.getBuffer(rt);
            var x = 1.0001f;

            var padding = -0.0025f; // padding to remove black outline
            var zStart = 1 - 15 / 16f + padding;
            var zEnd = 1 - 1 / 16f - padding;
            var yEnd = 1 - 3 / 16f - padding;
            var yStart = 1 - 13 / 16f + padding;
            //quad(buf, pPoseStack.last(), v(x, yEnd, zStart), v(x, yStart, zStart), v(x, yStart, zEnd), v(x, yEnd, zEnd), color);
            quad(buf, pPoseStack.last(), v(x, yEnd, zEnd), v(x, yStart, zEnd), v(x, yStart, zStart), v(x, yEnd, zStart), Color.BLUE);
        } else {
            String textToRender = pBlockEntity.guiContent;//"Some \ntext \nthat \nwe \nwant \nto \nrender :)";
            var lines = textToRender.split("\n", -1);
            int lineCount = lines.length - 1;//textToRender.length() - textToRender.replace("\n", "").length() + 1;
            var font = ClientStatics.getMonoFont();
            float charWidth = font.width("a");
            int maxCharsWidth = 110; // 110 by 40 roughly
            int maxCharsHeight = 40;
            float screenScale = (14 / 16f) / ((1 /*=padding*/ + maxCharsWidth) * charWidth);// 0.002f;
            float expectedTextHeight = font.lineHeight * lineCount;
            float offsetToCenterTheTextVertically = expectedTextHeight / 2f;

            float paddingTop = (8 / 16f - expectedTextHeight * screenScale / 2f);
            float paddingLeft = charWidth * 0.5f * screenScale;

            pPoseStack.pushPose();
            pPoseStack.translate(1, 1 - paddingTop - 0.0015f, 1 - 1 / 16f - paddingLeft);
            pPoseStack.mulPose(new Quaternionf().rotateY((float) (-Math.PI / 2d)));
            pPoseStack.mulPose(new Quaternionf().rotateZ((float) (Math.PI)));
            pPoseStack.mulPose(new Quaternionf().scale(screenScale));
            for (int iLine = 0; iLine<lineCount; iLine++) {
                font.drawInBatch(lines[iLine], 0, iLine*font.lineHeight, 0xFFFFFF, false,
                        pPoseStack.last().pose(), pBuffer, Font.DisplayMode.POLYGON_OFFSET, 0, 15728880);
            }

            pPoseStack.popPose();
        }
        pPoseStack.popPose();
    }

    static void poseStack_mulFacing(PoseStack ps, Direction dir) {
        var f = Axis.YP.rotationDegrees(switch (dir) {
            case EAST -> 0;
            case SOUTH -> 270;
            case WEST -> 180;
            default -> 90;
        });
        ps.mulPose(f);
    }

    static RenderType rt = RenderType.create("solid", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.QUADS,
            2097152, true, false, RenderType.CompositeState.builder()
                    .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTextureState(new RenderStateShard.EmptyTextureStateShard(() -> {
                    }, () -> {
                    }))
                    .createCompositeState(true));

    public static void quad(VertexConsumer v, PoseStack.Pose pose, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, Color color) {
        //Vec3 normal = v3.subtract(v2).cross(v1.subtract(v2)).normalize();
        Matrix3f normal = pose.normal();
        Matrix4f m4 = pose.pose();

        putVertex(v, m4, normal, v1.x, v1.y, v1.z, color);
        putVertex(v, m4, normal, v2.x, v2.y, v2.z, color);
        putVertex(v, m4, normal, v3.x, v3.y, v3.z, color);
        putVertex(v, m4, normal, v4.x, v4.y, v4.z, color);
    }

    private static void putVertex(VertexConsumer builder, Matrix4f pose, Matrix3f normal,
                                  double x, double y, double z, Color color) {
        builder.vertex(pose, (float) x, (float) y, (float) z)
                .color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha())
                //               .uv(0, 0)
//                .overlayCoords(OverlayTexture.NO_OVERLAY)

//                        .uv2(0xFFFF, 0xFFFF)
                .normal(normal, 0, 1, 0)
                //.normal((float) normal.x(), (float) normal.y(), (float) normal.z())
                .endVertex();
    }

    public static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }
}
