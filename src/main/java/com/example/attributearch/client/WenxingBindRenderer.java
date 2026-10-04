package com.example.attributearch.client;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.effect.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

@EventBusSubscriber(modid = AttributeArch.MODID, value = Dist.CLIENT)
public final class WenxingBindRenderer {
    private WenxingBindRenderer() {
    }

    public static final ResourceLocation BIND_ICON =
            ResourceLocation.fromNamespaceAndPath("attributearch", "textures/mob_effect/wenxing_bind.png");

    private static final float ICON_SIZE = 0.28F;

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.WENXING_BIND)) {
            return;
        }
        renderIcon(event.getPoseStack(), event.getMultiBufferSource(), entity);
    }

    private static void renderIcon(PoseStack pose, MultiBufferSource buffers, LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer.getMainCamera() == null) {
            return;
        }

        pose.pushPose();

        pose.translate(0.0D, entity.getBbHeight() + 0.35D, 0.0D);

        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(ICON_SIZE, ICON_SIZE, ICON_SIZE);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(BIND_ICON));
        int light = LightTexture.FULL_BRIGHT;

        vertex(vc, pose, -0.5F, 0.5F, 0.0F, 0.0F, 0.0F, light);
        vertex(vc, pose, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F, light);
        vertex(vc, pose, 0.5F, -0.5F, 0.0F, 1.0F, 1.0F, light);
        vertex(vc, pose, 0.5F, 0.5F, 0.0F, 1.0F, 0.0F, light);

        pose.popPose();
    }

    private static void vertex(VertexConsumer vc, PoseStack pose,
                               float x, float y, float z, float u, float v, int light) {
        vc.addVertex(pose.last(), x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0F, 0.0F, 1.0F);
    }
}
