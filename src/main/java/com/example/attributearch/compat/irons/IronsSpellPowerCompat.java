package com.example.attributearch.compat.irons;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.attribute.AttributeHelper;
import com.example.attributearch.attribute.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class IronsSpellPowerCompat {
    private static final boolean LOADED = ModList.get().isLoaded("irons_spellbooks");

    private static final ResourceLocation IRON_TRANSFER_ADD =
            ResourceLocation.fromNamespaceAndPath(AttributeArch.MODID, "iron_transfer/add");

    private IronsSpellPowerCompat() {
    }

    public static void init() {
        if (!LOADED) {
            return;
        }
        AttributeArch.LOGGER.info("Iron's Spellbooks amplification compat enabled");
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static void sync(Player player) {
        if (!LOADED || player == null || player.level().isClientSide()) {
            return;
        }
        Holder<Attribute> spellPower = resolveSpellPower();
        if (spellPower == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(spellPower);
        if (instance == null) {
            return;
        }

        instance.removeModifier(IRON_TRANSFER_ADD);

        if (AttributeHelper.getIronAmplificationLevel(player) < 1) {
            return;
        }
        double meleeTotal = ModAttributes.getMeleeAttackDamageTotal(player);
        if (meleeTotal <= 0.0D) {
            return;
        }
        instance.addOrReplacePermanentModifier(new AttributeModifier(
                IRON_TRANSFER_ADD,
                meleeTotal,
                AttributeModifier.Operation.ADD_VALUE));
    }

    public static void clear(Player player) {
        if (!LOADED || player == null) {
            return;
        }
        Holder<Attribute> spellPower = resolveSpellPower();
        if (spellPower == null) {
            return;
        }
        AttributeInstance instance = player.getAttribute(spellPower);
        if (instance == null) {
            return;
        }
        instance.removeModifier(IRON_TRANSFER_ADD);
    }

    private static Holder<Attribute> resolveSpellPower() {
        try {
            return BuiltInRegistries.ATTRIBUTE
                    .getHolder(ResourceLocation.parse("irons_spellbooks:spell_power"))
                    .orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
