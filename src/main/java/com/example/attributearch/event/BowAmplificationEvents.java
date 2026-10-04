package com.example.attributearch.event;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.attribute.ModAttributes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * 弓增幅：最终伤害 = 弓箭造成的伤害 + 近战伤害（所有弓箭，不限 wenxing 弓）。
 */
@EventBusSubscriber(modid = AttributeArch.MODID)
public final class BowAmplificationEvents {

    private BowAmplificationEvents() {
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!isBowArrowDamage(event.getSource())) {
            return;
        }
        Player player = resolvePlayer(event.getSource());
        if (player == null || !ModAttributes.isBowAmplificationEnabled(player)) {
            return;
        }
        double meleeTotal = ModAttributes.getMeleeAttackDamageTotal(player);
        if (meleeTotal <= 0.0D) {
            return;
        }
        event.setAmount((float) (event.getAmount() + meleeTotal));
    }

    private static boolean isBowArrowDamage(DamageSource source) {
        return source.getDirectEntity() instanceof AbstractArrow
                || source.getEntity() instanceof AbstractArrow;
    }

    private static Player resolvePlayer(DamageSource source) {
        Entity attacker = source.getEntity();
        if (attacker instanceof Projectile projectile && projectile.getOwner() != null) {
            attacker = projectile.getOwner();
        }
        return attacker instanceof Player player ? player : null;
    }
}
