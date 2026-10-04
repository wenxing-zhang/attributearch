package com.example.attributearch.attribute;

import java.util.HashMap;
import java.util.Map;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.attachment.ModAttachments;
import com.example.attributearch.config.ModConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

public final class AttributeHelper {
    private AttributeHelper() {
    }

    public static int getLevel(Player player, ResourceLocation attrId) {
        if (player == null || attrId == null) {
            return 0;
        }
        try {
            Map<ResourceLocation, Integer> map = player.getData(ModAttachments.ENHANCE_LEVELS);
            if (map == null) {
                return 0;
            }
            return map.getOrDefault(attrId, 0);
        } catch (Exception ex) {
            AttributeArch.LOGGER.warn("Failed to read enhance level for {}", attrId, ex);
            return 0;
        }
    }

    public static int getBowAmplificationLevel(Player player) {
        return getLevel(player, ModAttributes.BOW_AMPLIFICATION_ID);
    }

    public static int getWitchAmplificationLevel(Player player) {
        return getLevel(player, ModAttributes.WITCH_AMPLIFICATION_ID);
    }

    public static int getIronAmplificationLevel(Player player) {
        return getLevel(player, ModAttributes.IRON_AMPLIFICATION_ID);
    }

    public static int getMaxLevel(ResourceLocation attrId) {
        return ModAttributes.isAmplification(attrId)
                ? ModAttributes.MAX_AMPLIFICATION_LEVEL
                : ModConfig.MAX_LEVEL.get();
    }

    public static int getCostForNextLevel(int currentLevel) {

        return Math.max(1, ModConfig.XP_COST_PER_LEVEL.get() * (currentLevel + 1));
    }

    public static void setLevel(Player player, ResourceLocation attrId, int level) {
        if (player == null || attrId == null) {
            return;
        }
        try {
            int clamped = Math.max(0, level);
            Map<ResourceLocation, Integer> map = new HashMap<>(player.getData(ModAttachments.ENHANCE_LEVELS));
            if (clamped <= 0) {
                map.remove(attrId);
            } else {
                map.put(attrId, clamped);
            }
            player.setData(ModAttachments.ENHANCE_LEVELS, map);
        } catch (Exception ex) {
            AttributeArch.LOGGER.warn("Failed to write enhance level for {}", attrId, ex);
        }
    }

    public static boolean tryEnhance(Player player, ResourceLocation attrId) {
        if (player == null || attrId == null || player.level().isClientSide) {
            return false;
        }
        try {
            if (!EnhanceableAttributes.isAllowed(attrId)) {
                return false;
            }
            int current = getLevel(player, attrId);
            int max = getMaxLevel(attrId);
            if (current >= max) {
                return false;
            }
            int cost = getCostForNextLevel(current);
            if (player.experienceLevel < cost && !player.getAbilities().instabuild) {
                return false;
            }
            if (!player.getAbilities().instabuild) {
                player.giveExperienceLevels(-cost);
            }
            setLevel(player, attrId, current + 1);
            apply(player, attrId);
            return true;
        } catch (Exception ex) {
            AttributeArch.LOGGER.warn("tryEnhance failed for {}", attrId, ex);
            return false;
        }
    }

    public static boolean tryDowngrade(Player player, ResourceLocation attrId) {
        if (player == null || attrId == null || player.level().isClientSide) {
            return false;
        }
        try {
            int current = getLevel(player, attrId);
            if (current <= 0) {
                return false;
            }
            setLevel(player, attrId, current - 1);
            apply(player, attrId);
            return true;
        } catch (Exception ex) {
            AttributeArch.LOGGER.warn("tryDowngrade failed for {}", attrId, ex);
            return false;
        }
    }

    public static void apply(Player player, ResourceLocation attrId) {
        if (player == null || attrId == null) {
            return;
        }
        try {
            Holder.Reference<Attribute> holder = BuiltInRegistries.ATTRIBUTE.getHolder(attrId).orElse(null);
            if (holder == null) {
                AttributeArch.LOGGER.debug("Unknown attribute {} when applying modifiers", attrId);
                return;
            }
            AttributeInstance instance = player.getAttribute(holder);
            if (instance == null) {
                return;
            }

            int level = getLevel(player, attrId);
            ResourceLocation addId = modifierId("add", attrId);
            ResourceLocation mulId = modifierId("mul", attrId);
            ResourceLocation indId = modifierId("ind", attrId);

            if (ModAttributes.isAmplification(attrId)) {
                instance.removeModifier(addId);
                instance.removeModifier(mulId);
                instance.removeModifier(indId);
                if (ModAttributes.isBowAmplification(attrId)) {
                    clearBowTransferModifiers(player);
                }
                if (ModAttributes.isWitchAmplification(attrId) || ModAttributes.isIronAmplification(attrId)
                        || ModAttributes.isBowAmplification(attrId)) {
                    com.example.attributearch.compat.spell.SpellAmplificationCompat.sync(player);
                }
                return;
            }

            if (level <= 0) {
                instance.removeModifier(addId);
                instance.removeModifier(mulId);
                instance.removeModifier(indId);
                if (isAttackDamage(attrId)) {
                    clearBowTransferModifiers(player);
                    com.example.attributearch.compat.spell.SpellAmplificationCompat.sync(player);
                }
                return;
            }

            double addAmount = ModConfig.BASE_ADDITION_PER_LEVEL.get() * level;
            double mulAmount = ModConfig.BASE_MULTIPLIER_PER_LEVEL.get() * level;
            double indAmount = ModConfig.BASE_INDEPENDENT_MULTIPLIER_PER_LEVEL.get() * level;

            instance.addOrReplacePermanentModifier(new AttributeModifier(
                    addId,
                    addAmount,
                    AttributeModifier.Operation.ADD_VALUE));
            if (mulAmount != 0.0D) {
                instance.addOrReplacePermanentModifier(new AttributeModifier(
                        mulId,
                        mulAmount,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else {
                instance.removeModifier(mulId);
            }
            if (indAmount != 0.0D) {
                instance.addOrReplacePermanentModifier(new AttributeModifier(
                        indId,
                        indAmount,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                instance.removeModifier(indId);
            }
            if (isAttackDamage(attrId)) {
                clearBowTransferModifiers(player);
                com.example.attributearch.compat.spell.SpellAmplificationCompat.sync(player);
            }
        } catch (Exception ex) {
            AttributeArch.LOGGER.warn("Failed to apply enhance modifiers for {}", attrId, ex);
        }
    }

    public static void clearBowTransferModifiers(Player player) {
        if (player == null) {
            return;
        }
        try {
            Holder<Attribute> projectile = ModAttributes.resolveProjectileDamageAttribute(player);
            AttributeInstance instance = projectile == null ? null : player.getAttribute(projectile);
            if (instance == null) {
                return;
            }
            ResourceLocation addId = ResourceLocation.fromNamespaceAndPath(AttributeArch.MODID, "bow_transfer/add");
            ResourceLocation mulId = ResourceLocation.fromNamespaceAndPath(AttributeArch.MODID, "bow_transfer/mul");
            ResourceLocation indId = ResourceLocation.fromNamespaceAndPath(AttributeArch.MODID, "bow_transfer/ind");
            instance.removeModifier(addId);
            instance.removeModifier(mulId);
            instance.removeModifier(indId);
        } catch (Exception ex) {
            AttributeArch.LOGGER.warn("Failed to clear bow transfer modifiers", ex);
        }
    }

    private static boolean isAttackDamage(ResourceLocation attrId) {
        return attrId != null && (
                "minecraft:generic.attack_damage".equals(attrId.toString())
                        || "attack_damage".equals(attrId.getPath()));
    }

    public static void applyAll(Player player) {
        if (player == null) {
            return;
        }
        try {
            Map<ResourceLocation, Integer> map = player.getData(ModAttachments.ENHANCE_LEVELS);
            if (map != null) {
                for (ResourceLocation attrId : map.keySet()) {
                    apply(player, attrId);
                }
            }
            apply(player, ModAttributes.BOW_AMPLIFICATION_ID);
            apply(player, ModAttributes.GUN_AMPLIFICATION_ID);
            apply(player, ModAttributes.WITCH_AMPLIFICATION_ID);
            apply(player, ModAttributes.IRON_AMPLIFICATION_ID);
            clearBowTransferModifiers(player);
            com.example.attributearch.compat.spell.SpellAmplificationCompat.sync(player);
        } catch (Exception ex) {
            AttributeArch.LOGGER.warn("Failed to applyAll enhance modifiers", ex);
        }
    }

    private static ResourceLocation modifierId(String kind, ResourceLocation attrId) {
        return ResourceLocation.fromNamespaceAndPath(
                AttributeArch.MODID,
                kind + "/" + attrId.getNamespace() + "/" + attrId.getPath());
    }
}
