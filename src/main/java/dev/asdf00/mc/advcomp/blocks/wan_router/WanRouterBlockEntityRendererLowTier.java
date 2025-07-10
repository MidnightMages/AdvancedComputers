package dev.asdf00.mc.advcomp.blocks.wan_router;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class WanRouterBlockEntityRendererLowTier implements BlockEntityRenderer<WanRouterBlockEntityLowTier> {
    public WanRouterBlockEntityRendererLowTier(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(@NotNull WanRouterBlockEntityLowTier pBlockEntity) {
        return false;
    }

    private float time = 0;
    private long lastTimeStamp = 0;
    private static final float div = 1_000;

    @Override
    public void render(@NotNull WanRouterBlockEntityLowTier pBlockEntity, float pPartialTick, @NotNull PoseStack pPoseStack,
                       @NotNull MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay) {
        pPoseStack.pushPose();

        var currentTime = System.currentTimeMillis();
        var delta = currentTime - lastTimeStamp;
        lastTimeStamp = currentTime;

        time += delta / div;
        time %= 2 * (float) Math.PI;
        var depth = (float) Math.sin(time) * 0.04f;

        for (int i = 0; i < 6; i++) {
            pPoseStack.translate(0.5f, 0.5f, 0.5f);
            if (i < 4)
                pPoseStack.mulPose(Axis.YP.rotationDegrees(90));
            else
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(i == 4 ? 90 : 180));
            pPoseStack.translate(-0.5f, -0.5f, -0.5f);
            renderEnderFace(pPoseStack, pBuffer, depth);
        }

        pPoseStack.popPose();
    }

    private static void renderEnderFace(@NotNull PoseStack pPoseStack, @NotNull MultiBufferSource pBuffer, float depth) {
        var buf = pBuffer.getBuffer(rt);
        depth -= 0.25f/16f;
        var x = 1 - 3f / 16f - depth;
        var width = 10f - depth*32f;
        var zStart = (1-width/16f)/2f;
        var yStart = zStart;
        var zEnd = zStart + width/16f;
        var yEnd = zEnd;
        quad(buf, pPoseStack.last(), v(x, yEnd, zEnd), v(x, yStart, zEnd), v(x, yStart, zStart), v(x, yEnd, zStart));
    }

    static RenderType rt = RenderType.endGateway();


    public static void quad(VertexConsumer v, PoseStack.Pose pose, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4) {
        Matrix4f m4 = pose.pose();

        putVertex(v, m4, v1.x, v1.y, v1.z);
        putVertex(v, m4, v2.x, v2.y, v2.z);
        putVertex(v, m4, v3.x, v3.y, v3.z);
        putVertex(v, m4, v4.x, v4.y, v4.z);
    }

    private static void putVertex(VertexConsumer builder, Matrix4f pose, double x, double y, double z) {
        builder.vertex(pose, (float) x, (float) y, (float) z).endVertex();
    }

    public static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }
}
