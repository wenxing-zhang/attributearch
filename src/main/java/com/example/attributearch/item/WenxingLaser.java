package com.example.attributearch.item;

import com.example.attributearch.effect.ModEffects;
import com.example.attributearch.entity.WenxingBeamArrow;
import com.example.attributearch.entity.WenxingBeamArrowTracker;
import com.example.attributearch.totem.WenxingBindTracker;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public final class WenxingLaser {

    public static final double MAX_RANGE = 256.0D;
    public static final int BIND_DURATION_TICKS = 600;
    public static final int SLOWNESS_AMPLIFIER = 1000;
    public static final double BASE_DAMAGE = 1000.0D;
    public static final double PROJECTILE_DAMAGE_BONUS = 1000.0D;
    public static final double CRIT_MULTIPLIER = 1.5D;
    public static final float BONUS_DAMAGE_RATIO = 1.0F;
    public static final int HITS_PER_SECOND = 4000;
    public static final int HITS_PER_TICK = 200;
    private static final ResourceKey<DamageType> VOID_ARROW =
            ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("attributearch", "void_arrow"));

    private WenxingLaser() {
    }

    /** 近战分量 L：完整近战攻击力 + 武器附魔（锋利/亡灵克星等）加成。 */
    public static float meleeDamage(Player player, LivingEntity target, ItemStack weapon) {
        float value = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            DamageSource source = player.damageSources().playerAttack(player);
            value = EnchantmentHelper.modifyDamage(serverLevel, weapon, target, source, value);
        }
        return value;
    }

    /** 弹射物分量 B：基础伤 + 力量 + 弹射物伤害，再乘暴击倍率。 */
    public static double bowDamage(Player player, ItemStack bow, int powerLevel) {
        double powerBonus = powerLevel > 0 ? 0.5D * (powerLevel + 1) : 0.0D;
        double projectile = com.example.attributearch.attribute.ModAttributes.getProjectileDamage(player)
                + PROJECTILE_DAMAGE_BONUS;
        return (BASE_DAMAGE + powerBonus + projectile) * CRIT_MULTIPLIER;
    }

    public static void hit(Player player, ItemStack bow, LivingEntity target, int powerLevel, boolean flame) {
        hit(player, bow, target, powerLevel, flame, true);
    }

    public static void hit(Player player, ItemStack bow, LivingEntity target, int powerLevel, boolean flame, boolean applySideEffects) {
        Level level = player.level();
        if (level.isClientSide || target == null || !target.isAlive()) {
            return;
        }

        boolean dummy = WenxingBindTracker.isTrainingDummy(target);

        float meleePart = meleeDamage(player, target, bow);

        // 近战伤害：走 Player.attack 管线（原版/模组暴击、附魔、词条）
        target.invulnerableTime = 0;
        player.resetAttackStrengthTicker();
        player.attack(target);

        // 弹射物伤害 B：挂在文星弹上，便于弓增幅等「认箭」逻辑生效
        double projectilePart = bowDamage(player, bow, powerLevel);
        if (projectilePart > 0.0D && target.isAlive()) {
            WenxingBeamArrow bolt = WenxingBeamArrowTracker.getOrCreate(player, target);
            DamageSource source = bolt != null
                    ? projectileSource(player, bolt)
                    : player.damageSources().playerAttack(player);
            target.invulnerableTime = 0;
            boolean damaged = target.hurt(source, (float) projectilePart);
            if (!damaged && !dummy) {
                forceDamage(target, source, (float) projectilePart, player);
            }
        }

        // 弓伤害 = 弹射物伤害 + 近战伤害；额外 = 1×虚空 + 1×魔法
        float bowDamageTotal = meleePart + (float) projectilePart;
        if (bowDamageTotal > 0.0F && target.isAlive()) {
            applyBonusDamage(player, target, bowDamageTotal * BONUS_DAMAGE_RATIO, dummy);
        }

        if (!applySideEffects || dummy || !target.isAlive()) {
            return;
        }

        if (flame || hasFireAspect(player.level(), bow)) {
            target.igniteForSeconds(100.0F);
        }

        WenxingBindTracker.apply(target, BIND_DURATION_TICKS);
        WenxingBindTracker.forceEffect(target, new MobEffectInstance(
                ModEffects.WENXING_BIND, BIND_DURATION_TICKS, 0, false, true, true));
        target.addEffect(new MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, BIND_DURATION_TICKS,
                SLOWNESS_AMPLIFIER, false, true, true));
    }

    private static DamageSource projectileSource(Player player, WenxingBeamArrow bolt) {
        return player.damageSources().source(
                net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK, bolt, player);
    }

    private static void applyBonusDamage(Player player, LivingEntity target, float amount, boolean dummy) {
        DamageSource magicSource = player.damageSources().magic();
        DamageSource voidSource = player.damageSources().source(VOID_ARROW, player);
        target.invulnerableTime = 0;
        boolean magicDamaged = target.hurt(magicSource, amount);
        if (!magicDamaged && !dummy) {
            forceDamage(target, magicSource, amount, player);
        }
        if (!target.isAlive()) {
            return;
        }
        target.invulnerableTime = 0;
        boolean voidDamaged = target.hurt(voidSource, amount);
        if (!voidDamaged && !dummy) {
            forceDamage(target, voidSource, amount, player);
        }
    }

    private static boolean hasFireAspect(Level level, ItemStack bow) {
        try {
            var holder = level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                    .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT);
            return EnchantmentHelper.getItemEnchantmentLevel(holder, bow) > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean forceDamage(LivingEntity living, DamageSource source, float damage, Player owner) {
        boolean prevInv = living.isInvulnerable();
        boolean prevAbilities = false;
        Player playerRef = null;
        living.setInvulnerable(false);
        living.invulnerableTime = 0;
        if (living instanceof Player player) {
            playerRef = player;
            prevAbilities = player.getAbilities().invulnerable;
            player.getAbilities().invulnerable = false;
        }
        DamageSource lootSource = owner != null
                ? living.damageSources().playerAttack(owner)
                : living.damageSources().generic();
        try {
            // 只投递一次 hurt：失败后直接扣血，不再补发伤害事件，避免双发
            if (living.hurt(source, damage)) {
                return true;
            }
            float before = living.getHealth();
            living.setHealth(Math.max(0.0F, before - damage));
            boolean applied = living.getHealth() < before;
            if (living.isDeadOrDying() && !living.isRemoved()) {
                if (owner != null) {
                    living.setLastHurtByPlayer(owner);
                    setLastHurtByPlayerTime(living, 100);
                }
                living.die(lootSource);
            }
            return applied || living.isDeadOrDying();
        } finally {
            living.setInvulnerable(prevInv);
            if (playerRef != null) {
                playerRef.getAbilities().invulnerable = prevAbilities;
            }
        }
    }

    private static void setLastHurtByPlayerTime(LivingEntity living, int time) {
        try {
            java.lang.reflect.Field field = LivingEntity.class.getDeclaredField("lastHurtByPlayerTime");
            field.setAccessible(true);
            field.setInt(living, time);
        } catch (Throwable ignored) {
        }
    }
}
