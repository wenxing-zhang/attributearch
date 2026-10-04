package com.example.attributearch.item;

import com.example.attributearch.enchant.EnchantingAltarHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class WenxingBowItem extends BowItem {

    public static final double BASE_DAMAGE = WenxingLaser.BASE_DAMAGE;
    public static final double PROJECTILE_DAMAGE_BONUS = WenxingLaser.PROJECTILE_DAMAGE_BONUS;
    public static final double CRIT_MULTIPLIER = WenxingLaser.CRIT_MULTIPLIER;
    public static final double DRAW_TICKS = 0.05D;

    public WenxingBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int timeLeft) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        int used = this.getUseDuration(stack, entity) - timeLeft;
        if (used < (int) DRAW_TICKS) {
            return;
        }
        fireLaser(stack, player, used <= 1);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        int used = this.getUseDuration(stack, entity) - timeLeft;
        if (used <= (int) DRAW_TICKS) {
            fireLaser(stack, player, true);
        }
    }

    private void fireLaser(ItemStack bow, Player player, boolean playSound) {
        if (player.getCooldowns().isOnCooldown(this)) {
            return;
        }
        LivingEntity aimed = findAimedLivingEntity(player);
        int power = enchantmentLevel(player.level(), bow, Enchantments.POWER);
        int flame = enchantmentLevel(player.level(), bow, Enchantments.FLAME);
        if (aimed != null) {
            for (int i = 0; i < WenxingLaser.HITS_PER_TICK; i++) {
                WenxingLaser.hit(player, bow, aimed, power, flame > 0, i == 0);
            }
        }
        if (playSound) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F,
                    1.0F / (player.level().getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }

    private static LivingEntity findAimedLivingEntity(Player player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        double reach = WenxingLaser.MAX_RANGE;
        Vec3 end = eye.add(look.scale(reach));
        AABB search = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, search, entity -> {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
                return false;
            }
            if (living.is(player) || living.isPassengerOfSameVehicle(player)) {
                return false;
            }
            return living.isPickable() && player.hasLineOfSight(living);
        });
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static int enchantmentLevel(Level level, ItemStack stack, ResourceKey<Enchantment> key) {
        try {
            Holder<Enchantment> holder = level.registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(key);
            return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return allowsEnchantment(enchantment);
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return allowsEnchantment(enchantment);
    }

    private static boolean allowsEnchantment(Holder<Enchantment> enchantment) {
        return enchantment != null
                && EnchantingAltarHelper.isDiscoverable(enchantment)
                && (isBowEnchantment(enchantment)
                || isMeleeWeaponEnchantment(enchantment)
                || isWenxingToolsWeaponEnchantment(enchantment));
    }

    private static boolean isBowEnchantment(Holder<Enchantment> enchantment) {
        try {
            ItemStack bow = new ItemStack(Items.BOW);
            return enchantment.value().canEnchant(bow)
                    || enchantment.value().isSupportedItem(bow)
                    || bow.supportsEnchantment(enchantment);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isMeleeWeaponEnchantment(Holder<Enchantment> enchantment) {
        try {
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            return enchantment.value().canEnchant(sword)
                    || enchantment.value().isSupportedItem(sword)
                    || sword.supportsEnchantment(enchantment);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isWenxingToolsWeaponEnchantment(Holder<Enchantment> enchantment) {
        ResourceLocation id = EnchantingAltarHelper.idOf(enchantment);
        if (id == null || !"wenxingtools".equals(id.getNamespace())) {
            return false;
        }
        return switch (id.getPath()) {
            case "attack_amplification",
                 "echo_amplification",
                 "effect_purge",
                 "resource_amplification",
                 "spawn_egg_harvest" -> true;
            default -> false;
        };
    }
}
