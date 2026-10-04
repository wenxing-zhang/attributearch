package com.example.attributearch.event;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.attribute.AttributeHelper;
import com.example.attributearch.attribute.ModAttributes;
import com.example.attributearch.compat.spell.SpellAmplificationCompat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = AttributeArch.MODID)
public final class SpellAmplificationEvents {

    private static final Map<UUID, Double> LAST_MELEE = new ConcurrentHashMap<>();
    private static final int DIRTY_CHECK_INTERVAL = 10;

    private SpellAmplificationEvents() {
    }

    @SubscribeEvent
    static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (event.getSlot() == EquipmentSlot.MAINHAND || event.getSlot() == EquipmentSlot.OFFHAND) {
            SpellAmplificationCompat.sync(player);
            cacheMelee(player);
        }
    }

    @SubscribeEvent
    static void onEffectAdd(MobEffectEvent.Added event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        SpellAmplificationCompat.sync(player);
        cacheMelee(player);
    }

    @SubscribeEvent
    static void onEffectRemove(MobEffectEvent.Remove event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        SpellAmplificationCompat.sync(player);
        cacheMelee(player);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        if (player.tickCount % DIRTY_CHECK_INTERVAL != 0) {
            return;
        }
        if (!isAnySpellAmplificationEnabled(player)) {
            LAST_MELEE.remove(player.getUUID());
            return;
        }
        double melee = ModAttributes.getMeleeAttackDamageTotal(player);
        Double last = LAST_MELEE.get(player.getUUID());
        if (last != null && Math.abs(last - melee) < 1.0e-6D) {
            return;
        }
        SpellAmplificationCompat.sync(player);
        cacheMelee(player);
    }

    @SubscribeEvent
    static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_MELEE.remove(event.getEntity().getUUID());
        SpellAmplificationCompat.clear(event.getEntity());
    }

    private static boolean isAnySpellAmplificationEnabled(Player player) {
        return AttributeHelper.getWitchAmplificationLevel(player) >= 1
                || AttributeHelper.getIronAmplificationLevel(player) >= 1;
    }

    private static void cacheMelee(Player player) {
        LAST_MELEE.put(player.getUUID(), ModAttributes.getMeleeAttackDamageTotal(player));
    }
}
