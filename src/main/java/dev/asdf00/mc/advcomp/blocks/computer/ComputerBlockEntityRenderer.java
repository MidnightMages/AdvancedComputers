package dev.asdf00.mc.advcomp.blocks.computer;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.awt.*;

public class ComputerBlockEntityRenderer implements BlockEntityRenderer<ComputerBlockEntity> {
    public ComputerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }


    @Override
    public boolean shouldRenderOffScreen(ComputerBlockEntity pBlockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(ComputerBlockEntity pBlockEntity, Vec3 pCameraPos) {
        return true;
    }

    private static Color getRenderColor(ComputerBlockEntity be){
        return be.getBlockState().getValue(ComputerBlock.RUN_STATE).color;
    }

    @Override
    public void render(@NotNull ComputerBlockEntity pBlockEntity, float pPartialTick, @NotNull PoseStack pPoseStack,
                       @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        pPoseStack.pushPose();
        var facing = pBlockEntity.getBlockState().getValue(ComputerBlock.FACING);

        pPoseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack_mulFacing(pPoseStack, facing);
        pPoseStack.translate(-0.5f, -0.5f, -0.5f);
        //pPoseStack.mulPose(Axis.YP.rotationDegrees(-90));

//        BlockEntityRenderer<ComputerBlockEntity>.
//        pPoseStack.translate(0,2,0);
//        var vc = pBuffer.getBuffer(RenderType.solid());
//GameRenderer.
//        quad(vc, v(0.5, 0.5, 0.5), v(0.5, 1, 0.5), v(1, 0.5, 0.5), v(1, 1, 0.5));
//        quad(vc,  v(0.5, 1, 0.5),v(0.5, 0.5, 0.5), v(1, 0.5, 0.5), v(1, 1, 0.5));
//        quad(vc, v(0.5, 0.5, 0.5),  v(1, 0.5, 0.5),v(0.5, 1, 0.5), v(1, 1, 0.5));
//        quad(vc, v(0.5, 0.5, 0.5),  v(1, 0.5, 0.5), v(1, 1, 0.5),v(0.5, 1, 0.5));
//
//        pPoseStack.popPose();
//        var buf = pBuffer.getBuffer(rt);
//        quad(buf, v(0.5, 0.5, 0.5), v(0.5, 1, 0.5), v(1, 0.5, 0.5), v(1, 1, 0.5));
//

//        if (true) {
            var buf = pBuffer.getBuffer(rt());
            var x=1.0001f;
            var zStart = 2/16f;
            var zEnd = zStart + 4/16f;
            var yEnd = 1-2/16f;
            var yStart = 1-4/16f;
            var color = getRenderColor(pBlockEntity);
        quad(buf, pPoseStack.last(),v(x, yEnd, zStart), v(x, yStart, zStart), v(x, yStart, zEnd), v(x, yEnd, zEnd), color);
        quad(buf, pPoseStack.last(), v(x, yEnd, zEnd),v(x, yStart, zEnd), v(x, yStart, zStart),v(x, yEnd, zStart),  color);
//        } else {
//            var t = Tesselator.getInstance();
//            var buf = t.getBuilder();
//            buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);
//            quad(buf, pPoseStack.last(), v(0.5, 0.5, 0.5), v(0.5, 1, 0.5), v(0.5, 1, 1), v(0.5, 0.5, 1));
//
//            try (var vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC)) {
//                vertexBuffer.bind();
//                vertexBuffer.upload(buf.end());
//                vertexBuffer.draw();
//            }
//        }
        pPoseStack.popPose();
/*

//        pPoseStack.translate(0, 2, 0);
//        pPoseStack.scale(1, 1, 1);
//        pPoseStack.mulPose(Axis.XP.rotationDegrees(270));
//        var buf = pBuffer.getBuffer(rt);
//        t.end();
        pPoseStack.popPose();

//        Vec3 view = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
//
//        var tesselator = Tesselator.getInstance();
//        var buffer = tesselator.getBuilder();
//        VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
//        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_NORMAL);
//        quad(buffer, v(0.5, 0.5, 0.5), v(0.5, 1, 0.5), v(1, 0.5, 0.5), v(1, 1, 0.5));
////        buffer.vertex(0, 0, 0).color(1f, 1f, 1f, 1f).endVertex();
////        buffer.vertex(10, 10, 10).color(1f, 1f, 1f, 1f).endVertex();
//        vertexBuffer.bind();
//        vertexBuffer.upload(buffer.end());
//
//        PoseStack matrix = pPoseStack;
//        matrix.pushPose();
//        matrix.translate(-view.x, -view.y, -view.z);
//        var shader = GameRenderer.getPositionColorShader();
//        vertexBuffer.draw();
//        matrix.popPose();

*/
    }

    static void poseStack_mulFacing(PoseStack ps, Direction dir){
        var f2 =  dir.getRotation();
        var f = Axis.YP.rotationDegrees(switch (dir) {
            case EAST -> 0;
            case SOUTH -> 270;
            case WEST -> 180;
            default -> 90;
        });
        ps.mulPose(f);
    }

    static RenderType rt(){return RenderType.create("solid", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.QUADS,
            2097152, true, false, RenderType.CompositeState.builder()
                    .setLightmapState(new RenderStateShard.LightmapStateShard(true))
                    .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
                    .setTextureState(new RenderStateShard.EmptyTextureStateShard(() -> {
                    }, () -> {
                    }))
                    .createCompositeState(true));}

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
//        float iu = sprite.getU(u);
//        float iv = sprite.getV(v);
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
