package com.example.attributearch.compat.goety;

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

import java.util.ArrayList;
import java.util.List;

/**
 * 巫法增幅：近战总值以加法区加到 Goety 通用与九派系法术强效属性。
 */
public final class GoetySpellPotencyCompat {
    private static final boolean LOADED = ModList.get().isLoaded("goety");

    private static final ResourceLocation WITCH_TRANSFER_ADD =
            ResourceLocation.fromNamespaceAndPath(AttributeArch.MODID, "witch_transfer/add");

    private static final String[] POTENCY_IDS = {
            "goety:spell_potency",
            "goety:abyss_potency",
            "goety:frost_potency",
            "goety:geomancy_potency",
            "goety:necromancy_potency",
            "goety:nether_potency",
            "goety:storm_potency",
            "goety:void_potency",
            "goety:wild_potency",
            "goety:wind_potency"
    };

    private GoetySpellPotencyCompat() {
    }

    public static void init() {
        if (!LOADED) {
            return;
        }
        AttributeArch.LOGGER.info("Goety witch amplification compat enabled");
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static void sync(Player player) {
        if (!LOADED || player == null || player.level().isClientSide()) {
            return;
        }
        List<AttributeInstance> instances = collectPotencyInstances(player);
        for (AttributeInstance instance : instances) {
            instance.removeModifier(WITCH_TRANSFER_ADD);
        }
        if (AttributeHelper.getWitchAmplificationLevel(player) < 1) {
            return;
        }
        double meleeTotal = ModAttributes.getMeleeAttackDamageTotal(player);
        if (meleeTotal <= 0.0D) {
            return;
        }
        for (AttributeInstance instance : instances) {
            instance.addOrReplacePermanentModifier(new AttributeModifier(
                    WITCH_TRANSFER_ADD,
                    meleeTotal,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static void clear(Player player) {
        if (!LOADED || player == null) {
            return;
        }
        for (AttributeInstance instance : collectPotencyInstances(player)) {
            instance.removeModifier(WITCH_TRANSFER_ADD);
        }
    }

    private static List<AttributeInstance> collectPotencyInstances(Player player) {
        List<AttributeInstance> instances = new ArrayList<>(POTENCY_IDS.length);
        for (String id : POTENCY_IDS) {
            Holder<Attribute> attribute = resolveAttribute(id);
            if (attribute == null) {
                continue;
            }
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            instances.add(instance);
        }
        return instances;
    }

    private static Holder<Attribute> resolveAttribute(String id) {
        try {
            return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id)).orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
