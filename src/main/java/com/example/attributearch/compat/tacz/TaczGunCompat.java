package com.example.attributearch.compat.tacz;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.attribute.ModAttributes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class TaczGunCompat {
    private static final boolean LOADED = ModList.get().isLoaded("tacz");

    private static final TagKey<DamageType> TACZ_BULLETS = TagKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("tacz", "bullets"));

    private TaczGunCompat() {
    }

    public static void init() {
        if (!LOADED) {
            return;
        }
        NeoForge.EVENT_BUS.register(new TaczGunCompat());
        AttributeArch.LOGGER.info("TACZ gun amplification compat enabled");
    }

    @SubscribeEvent
    public void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!LOADED || event.getEntity().level().isClientSide()) {
            return;
        }
        if (!event.getSource().is(TACZ_BULLETS)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity living)) {
            return;
        }
        if (!ModAttributes.isGunAmplificationEnabled(living)) {
            return;
        }
        double add = ModAttributes.getMeleeAttackDamageTotal(living);
        if (add <= 0.0D) {
            return;
        }
        event.setAmount((float) (event.getAmount() + add));
    }
}
