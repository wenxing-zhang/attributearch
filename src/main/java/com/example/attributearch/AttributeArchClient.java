package com.example.attributearch;

import com.example.attributearch.client.AttributeAltarScreen;
import com.example.attributearch.client.ClientLevelCache;
import com.example.attributearch.client.EnchantingAltarScreen;
import com.example.attributearch.network.ClientHooks;
import com.example.attributearch.registry.ModItems;
import com.example.attributearch.registry.ModMenus;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AttributeArch.MODID, value = Dist.CLIENT)
public final class AttributeArchClient {
    private AttributeArchClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientHooks.ON_LEVELS_SYNC = ClientLevelCache::setLevels;
            ClientHooks.ON_LOGGED_OUT = ClientLevelCache::clear;
            registerBowItemProperties();
        });
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ATTRIBUTE_ALTAR.get(), AttributeAltarScreen::new);
        event.register(ModMenus.ENCHANTING_ALTAR.get(), EnchantingAltarScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                com.example.attributearch.registry.ModEntityTypes.WENXING_BEAM_ARROW.get(),
                com.example.attributearch.client.WenxingBeamArrowRenderer::new);
    }

    private static void registerBowItemProperties() {
        ItemProperties.register(ModItems.WENXING_BOW.get(),
                ResourceLocation.withDefaultNamespace("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null) {
                        return 0.0F;
                    }
                    return entity.getUseItem() != stack ? 0.0F
                            : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F;
                });
        ItemProperties.register(ModItems.WENXING_BOW.get(),
                ResourceLocation.withDefaultNamespace("pulling"),
                (stack, level, entity, seed) -> {
                    if (entity == null) {
                        return 0.0F;
                    }
                    return entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F;
                });
    }
}
