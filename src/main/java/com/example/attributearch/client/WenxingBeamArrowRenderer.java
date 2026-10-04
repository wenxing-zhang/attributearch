package com.example.attributearch.client;

import com.example.attributearch.entity.WenxingBeamArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class WenxingBeamArrowRenderer extends ArrowRenderer<WenxingBeamArrow> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public WenxingBeamArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(WenxingBeamArrow entity) {
        return TEXTURE;
    }
}
