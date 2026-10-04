package com.example.attributearch.event;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.config.ModConfig;
import com.example.attributearch.effect.ModEffects;
import com.example.attributearch.registry.ModItems;
import com.example.attributearch.totem.TotemFinder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = AttributeArch.MODID)
public final class WenxingTotemEvents {
    private WenxingTotemEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.hasEffect(ModEffects.WENXING_SHIELD)) {
            event.setCanceled(true);
            return;
        }
        if (event.getAmount() <= 0.0F) {
            return;
        }
        if (player.getHealth() + player.getAbsorptionAmount() - event.getAmount() > 0.0F) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(ModItems.WENXING_TOTEM.get())) {
            return;
        }
        if (!TotemFinder.hasWenxingTotem(player)) {
            return;
        }
        event.setCanceled(true);
        triggerTotem(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }

        if (player.hasEffect(ModEffects.WENXING_SHIELD)) {
            event.setCanceled(true);
            reviveFromDeath(player);
            return;
        }

        if (player.getCooldowns().isOnCooldown(ModItems.WENXING_TOTEM.get())) {
            return;
        }

        if (!TotemFinder.hasWenxingTotem(player)) {
            return;
        }

        triggerTotem(player);
        event.setCanceled(true);
        reviveFromDeath(player);
    }

    private static void reviveFromDeath(Player player) {
        if (player.getHealth() <= 0.0F) {
            player.setHealth(player.getMaxHealth());
        }
        try {
            player.getCombatTracker().recheckStatus();
        } catch (Throwable ignored) {

        }
    }

    private static void triggerTotem(Player player) {
        player.setHealth(player.getMaxHealth());

        if (ModConfig.WENXING_CLEAR_NEGATIVE_EFFECTS.get()) {
            List<MobEffectInstance> toRemove = new ArrayList<>();
            for (MobEffectInstance instance : player.getActiveEffects()) {
                if (instance.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) {
                    toRemove.add(instance);
                }
            }
            for (MobEffectInstance instance : toRemove) {
                player.removeEffect(instance.getEffect());
            }
        }

        player.addEffect(new MobEffectInstance(
                ModEffects.WENXING_SHIELD,
                ModConfig.WENXING_SHIELD_DURATION_TICKS.get(),
                0,
                true,
                true,
                true));

        player.getCooldowns().addCooldown(ModItems.WENXING_TOTEM.get(), ModConfig.WENXING_TOTEM_COOLDOWN_TICKS.get());

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverPlayer.level().broadcastEntityEvent(serverPlayer, (byte) 35);
            serverPlayer.displayClientMessage(
                    Component.translatable("message.attributearch.totem_triggered"), true);
        }
    }
}
