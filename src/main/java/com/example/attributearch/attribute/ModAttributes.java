package com.example.attributearch.attribute;

import com.example.attributearch.AttributeArch;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = AttributeArch.MODID)
public final class ModAttributes {
    private ModAttributes() {
    }

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, AttributeArch.MODID);

    public static final DeferredHolder<Attribute, Attribute> BOW_AMPLIFICATION =
            ATTRIBUTES.register("bow_amplification",
                    () -> new UncappedAttribute("attribute.name.attributearch.bow_amplification", 0.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> GUN_AMPLIFICATION =
            ATTRIBUTES.register("gun_amplification",
                    () -> new UncappedAttribute("attribute.name.attributearch.gun_amplification", 0.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> WITCH_AMPLIFICATION =
            ATTRIBUTES.register("witch_amplification",
                    () -> new UncappedAttribute("attribute.name.attributearch.witch_amplification", 0.0D).setSyncable(true));

    public static final DeferredHolder<Attribute, Attribute> IRON_AMPLIFICATION =
            ATTRIBUTES.register("iron_amplification",
                    () -> new UncappedAttribute("attribute.name.attributearch.iron_amplification", 0.0D).setSyncable(true));

    public static final int MAX_AMPLIFICATION_LEVEL = 1;

    public static final ResourceLocation BOW_AMPLIFICATION_ID =
            ResourceLocation.fromNamespaceAndPath("attributearch", "bow_amplification");

    public static final ResourceLocation GUN_AMPLIFICATION_ID =
            ResourceLocation.fromNamespaceAndPath("attributearch", "gun_amplification");

    public static final ResourceLocation WITCH_AMPLIFICATION_ID =
            ResourceLocation.fromNamespaceAndPath("attributearch", "witch_amplification");

    public static final ResourceLocation IRON_AMPLIFICATION_ID =
            ResourceLocation.fromNamespaceAndPath("attributearch", "iron_amplification");

    public static boolean isBowAmplification(ResourceLocation id) {
        return id != null && "attributearch".equals(id.getNamespace()) && "bow_amplification".equals(id.getPath());
    }

    public static boolean isGunAmplification(ResourceLocation id) {
        return id != null && "attributearch".equals(id.getNamespace()) && "gun_amplification".equals(id.getPath());
    }

    public static boolean isWitchAmplification(ResourceLocation id) {
        return id != null && "attributearch".equals(id.getNamespace()) && "witch_amplification".equals(id.getPath());
    }

    public static boolean isIronAmplification(ResourceLocation id) {
        return id != null && "attributearch".equals(id.getNamespace()) && "iron_amplification".equals(id.getPath());
    }

    public static boolean isAmplification(ResourceLocation id) {
        return isBowAmplification(id) || isGunAmplification(id) || isWitchAmplification(id) || isIronAmplification(id);
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, BOW_AMPLIFICATION);
        event.add(EntityType.PLAYER, GUN_AMPLIFICATION);
        event.add(EntityType.PLAYER, WITCH_AMPLIFICATION);
        event.add(EntityType.PLAYER, IRON_AMPLIFICATION);
    }

    public static double getUncappedValue(LivingEntity entity, Holder<Attribute> attribute) {
        if (entity == null || attribute == null) {
            return 0.0D;
        }
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return 0.0D;
        }
        double base = instance.getBaseValue();
        double add = 0.0D;
        double mulBase = 1.0D;
        double mulTotal = 1.0D;
        for (AttributeModifier modifier : instance.getModifiers()) {
            switch (modifier.operation()) {
                case ADD_VALUE -> add += modifier.amount();
                case ADD_MULTIPLIED_BASE -> mulBase += modifier.amount();
                case ADD_MULTIPLIED_TOTAL -> mulTotal *= (1.0D + modifier.amount());
            }
        }
        return (base + add) * mulBase * mulTotal;
    }

    public static Holder<Attribute> resolveProjectileDamageAttribute() {
        return findAttribute("attributeslib:arrow_damage");
    }

    public static Holder<Attribute> resolveProjectileDamageAttribute(LivingEntity entity) {
        Holder<Attribute> lib = findAttribute("attributeslib:arrow_damage");
        if (lib != null && (entity == null || entity.getAttribute(lib) != null)) {
            return lib;
        }
        return firstPresentOnEntity(entity, null, "playerex:arrow_damage");
    }

    public static double getProjectileDamage(LivingEntity entity) {
        Holder<Attribute> attr = resolveProjectileDamageAttribute(entity);
        if (entity == null || attr == null || entity.getAttribute(attr) == null) {
            return 0.0D;
        }
        return Math.max(0.0D, getUncappedValue(entity, attr));
    }

    public static Holder<Attribute> resolveProjectileSpeedAttribute() {
        return findAttribute("attributeslib:arrow_velocity");
    }

    public static Holder<Attribute> resolveProjectileSpeedAttribute(LivingEntity entity) {
        Holder<Attribute> lib = findAttribute("attributeslib:arrow_velocity");
        if (lib != null && (entity == null || entity.getAttribute(lib) != null)) {
            return lib;
        }
        return firstPresentOnEntity(entity, null, "playerex:projectile_speed");
    }

    public static double getProjectileSpeed(LivingEntity entity) {
        Holder<Attribute> attr = resolveProjectileSpeedAttribute(entity);
        if (entity == null || attr == null || entity.getAttribute(attr) == null) {
            return 0.0D;
        }
        return getUncappedValue(entity, attr);
    }

    public static boolean isBowAmplificationEnabled(Player player) {
        return player != null && AttributeHelper.getLevel(player, BOW_AMPLIFICATION_ID) >= MAX_AMPLIFICATION_LEVEL;
    }

    public static boolean isGunAmplificationEnabled(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof Player player) {
            return AttributeHelper.getLevel(player, GUN_AMPLIFICATION_ID) >= MAX_AMPLIFICATION_LEVEL;
        }
        return getUncappedValue(entity, GUN_AMPLIFICATION) > 0.0D;
    }

    public static boolean isWitchAmplificationEnabled(LivingEntity entity) {
        return entity instanceof Player player
                && AttributeHelper.getLevel(player, WITCH_AMPLIFICATION_ID) >= MAX_AMPLIFICATION_LEVEL;
    }

    public static boolean isIronAmplificationEnabled(LivingEntity entity) {
        return entity instanceof Player player
                && AttributeHelper.getLevel(player, IRON_AMPLIFICATION_ID) >= MAX_AMPLIFICATION_LEVEL;
    }

    public static double getMeleeAttackDamageTotal(LivingEntity entity) {
        return Math.max(0.0D, getUncappedValue(entity, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
    }

    private static Holder<Attribute> firstPresentOnEntity(LivingEntity entity, Holder<Attribute> preferred, String... ids) {
        if (entity != null && preferred != null && entity.getAttribute(preferred) != null) {
            return preferred;
        }
        for (String id : ids) {
            Holder<Attribute> attribute = findAttribute(id);
            if (attribute != null && entity != null && entity.getAttribute(attribute) != null) {
                return attribute;
            }
        }
        return preferred;
    }

    private static Holder<Attribute> findAttribute(String id) {
        try {
            return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(id)).orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
