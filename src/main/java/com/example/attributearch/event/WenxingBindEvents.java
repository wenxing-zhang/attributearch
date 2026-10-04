package com.example.attributearch.event;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.effect.ModEffects;
import com.example.attributearch.totem.WenxingBindTracker;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = AttributeArch.MODID)
public final class WenxingBindEvents {
    private WenxingBindEvents() {
    }

    private static boolean bound(LivingEntity entity) {
        return WenxingBindTracker.isBound(entity);
    }

    @SubscribeEvent
    public static void onEffectRemove(MobEffectEvent.Remove event) {

        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.WENXING_BIND)) {
            WenxingBindTracker.clear(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null
                && event.getEffectInstance().getEffect().is(ModEffects.WENXING_BIND)) {
            WenxingBindTracker.clear(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getItem().is(Items.MILK_BUCKET)) {
            WenxingBindTracker.clear(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            WenxingBindTracker.clear(living);
        }
    }

    @SubscribeEvent
    public static void onUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (bound(event.getEntity())) {

            if (!event.getItem().is(Items.MILK_BUCKET)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (bound(event.getEntity()) && !event.getItem().is(Items.MILK_BUCKET)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {

        WenxingBindTracker.clear(event.getEntity());
        Entity original = event.getOriginal();
        if (original instanceof LivingEntity living) {
            WenxingBindTracker.clear(living);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        WenxingBindTracker.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        WenxingBindTracker.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        WenxingBindTracker.clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WenxingBindTracker.tickAll();
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity raw = event.getEntity();
        if (!(raw instanceof LivingEntity entity) || entity.level().isClientSide) {
            return;
        }
        if (!bound(entity)) {
            return;
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;
        entity.setSprinting(false);
        if (entity.isFallFlying()) {
            stopFallFlying(entity);
        }
        WenxingBindTracker.freezeFlags(entity);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onJump(net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (bound(event.getEntity())) {
            event.getEntity().setDeltaMovement(Vec3.ZERO);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onKnockBack(net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent event) {
        if (bound(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void stopFallFlying(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            player.stopFallFlying();
            return;
        }
        try {
            java.lang.reflect.Method method = net.minecraft.world.entity.Entity.class
                    .getDeclaredMethod("setSharedFlag", int.class, boolean.class);
            method.setAccessible(true);
            method.invoke(entity, 7, false);
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof LivingEntity living && bound(living)) {
            event.setCanceled(true);
            return;
        }
        Entity cause = event.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectile
                ? projectile.getOwner() : null;
        if (cause instanceof LivingEntity living && bound(living)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker && bound(attacker)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerAttack(AttackEntityEvent event) {
        if (bound(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // NeoForge 1.21.1 的 PlayerInteractEvent 是抽象基类，不能用来注册监听器
    // （EventBus.addToListeners 会抛 IllegalArgumentException，导致 attributearch 构造失败、
    //  整个客户端进入 broken mod state，进而连累 FTB Quests 主题重载 NPE 崩溃）。
    // 抽象基类自身从不派发，只有下列具体子类会被派发，故逐个注册。
    // RightClickEmpty / LeftClickEmpty 不实现 ICancellableEvent，原实现对它们本就只会走到
    // “取不到可取消事件”这一步，等价于无操作，因此不再注册，以免语义被误读。
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        blockInteraction(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        blockInteraction(event);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        blockInteraction(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        blockInteraction(event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        blockInteraction(event);
    }

    private static void blockInteraction(PlayerInteractEvent event) {
        if (!bound(event.getEntity())) {
            return;
        }

        if (!event.getItemStack().isEmpty() && event.getItemStack().is(Items.MILK_BUCKET)) {
            return;
        }

        if (event instanceof ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }
    }
}
