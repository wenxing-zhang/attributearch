package com.example.attributearch.event;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.entity.WenxingBeamArrowTracker;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = AttributeArch.MODID)
public final class WenxingBeamArrowEvents {

    private WenxingBeamArrowEvents() {
    }

    @SubscribeEvent
    static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.level().isClientSide) {
            WenxingBeamArrowTracker.onTargetRemoved(living);
        }
    }

    @SubscribeEvent
    static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide) {
            WenxingBeamArrowTracker.onTargetRemoved(living);
        }
    }

    @SubscribeEvent
    static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        WenxingBeamArrowTracker.onPlayerGone(event.getEntity());
    }

    @SubscribeEvent
    static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        WenxingBeamArrowTracker.onPlayerGone(event.getEntity());
    }

    @SubscribeEvent
    static void onPlayerClone(PlayerEvent.Clone event) {
        WenxingBeamArrowTracker.onPlayerGone(event.getOriginal());
        WenxingBeamArrowTracker.onPlayerGone(event.getEntity());
    }
}
