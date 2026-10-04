package com.example.attributearch.event;

import java.util.HashMap;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.attachment.ModAttachments;
import com.example.attributearch.attribute.AttributeHelper;
import com.example.attributearch.attribute.EnhanceableAttributes;
import com.example.attributearch.config.ModConfig;
import com.example.attributearch.enchant.EnchantingAltarHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = AttributeArch.MODID)
public final class PlayerEvents {
    private PlayerEvents() {
    }

    @SubscribeEvent
    static void onClone(PlayerEvent.Clone event) {
        try {
            if (!(event.getOriginal() instanceof ServerPlayer original)
                    || !(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            if (event.isWasDeath() && !ModConfig.KEEP_ON_DEATH.get()) {
                player.setData(ModAttachments.ENHANCE_LEVELS, new HashMap<>());
                return;
            }
            mapSafeCopy(player, original);
        } catch (Exception ex) {
            AttributeArch.LOGGER.error("Player clone enhance-levels copy failed", ex);
        }
    }

    private static void mapSafeCopy(ServerPlayer player, ServerPlayer original) {
        try {
            player.setData(ModAttachments.ENHANCE_LEVELS,
                    new HashMap<>(original.getData(ModAttachments.ENHANCE_LEVELS)));
        } catch (Exception ex) {
            AttributeArch.LOGGER.error("Failed to copy enhance levels on clone", ex);
            player.setData(ModAttachments.ENHANCE_LEVELS, new HashMap<>());
        }
    }

    @SubscribeEvent
    static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            if (event.getEntity() instanceof ServerPlayer player) {
                AttributeHelper.applyAll(player);
            }
        } catch (Exception ex) {
            AttributeArch.LOGGER.error("LoggedIn enhance apply failed", ex);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        try {
            if (event.getEntity() instanceof ServerPlayer player) {
                AttributeHelper.applyAll(player);
            }
        } catch (Exception ex) {
            AttributeArch.LOGGER.error("Respawn enhance apply failed", ex);
        }
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        try {
            if (event.getEntity() instanceof ServerPlayer player) {
                AttributeHelper.applyAll(player);
            }
        } catch (Exception ex) {
            AttributeArch.LOGGER.error("Dimension change enhance apply failed", ex);
        }
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        EnhanceableAttributes.invalidateCache();
        EnchantingAltarHelper.invalidateCache();
    }
}
