package com.example.attributearch.compat.attributefix;

import com.example.attributearch.AttributeArch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

import java.lang.reflect.Field;
import java.util.List;

public final class AttributeFixBypass {

    private static final List<String> PROTECTED_IDS = List.of(
            "irons_spellbooks:cast_time_reduction",
            "irons_spellbooks:cooldown_reduction",
            "irons_spellbooks:mana_regen",
            "irons_spellbooks:max_mana",
            "irons_spellbooks:spell_power",
            "goety:casting_speed",
            "goety:cooldown_discount",
            "goety:soul_discount",
            "goety:spell_potency",
            "goety:abyss_potency",
            "goety:frost_potency",
            "goety:geomancy_potency",
            "goety:necromancy_potency",
            "goety:nether_potency",
            "goety:storm_potency",
            "goety:void_potency",
            "goety:wild_potency",
            "goety:wind_potency",
            "ars_nouveau:ars_nouveau.perk.mana_regen",
            "ars_nouveau:ars_nouveau.perk.max_mana",
            "ars_nouveau:perk.max_mana",
            "ars_nouveau:perk.mana_regen",
            "forge:swim_speed",
            "malum:soul_ward_capacity",
            "malum:soul_ward_integrity",
            "malum:soul_ward_recovery_rate",
            "malum:spirit_spoils",
            "minecraft:generic.flying_speed",
            "minecraft:generic.max_health",
            "minecraft:generic.armor",
            "minecraft:generic.armor_toughness",
            "minecraft:generic.attack_speed",
            "minecraft:generic.luck",
            "minecraft:generic.movement_speed",
            "attributeslib:creative_flight",
            "attributeslib:draw_speed",
            "attributeslib:arrow_damage",
            "attributeslib:arrow_velocity",
            "attributearch:bow_amplification",
            "attributearch:gun_amplification",
            "attributearch:witch_amplification",
            "attributearch:iron_amplification"
    );

    private AttributeFixBypass() {
    }

    public static void apply() {
        int restored = 0;
        for (String id : PROTECTED_IDS) {
            if (forceUncapped(id)) {
                restored++;
            }
        }
        if (restored > 0) {
            AttributeArch.LOGGER.info("AttributeFix bypass applied to {} attributes", restored);
        }
    }

    private static boolean forceUncapped(String id) {
        try {
            Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(ResourceLocation.parse(id));
            if (!(attribute instanceof RangedAttribute ranged)) {
                return false;
            }
            boolean changed = false;
            double maxValue = ranged.getMaxValue();
            for (Field field : RangedAttribute.class.getDeclaredFields()) {
                if (field.getType() != double.class) {
                    continue;
                }
                field.setAccessible(true);
                if (field.getDouble(ranged) == maxValue && maxValue < Double.MAX_VALUE) {
                    field.setDouble(ranged, Double.MAX_VALUE);
                    changed = true;
                    break;
                }
            }
            return changed;
        } catch (Throwable ex) {
            AttributeArch.LOGGER.warn("Failed to uncap attribute {}", id, ex);
            return false;
        }
    }
}
