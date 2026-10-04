package com.example.attributearch.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 每目标复用一支文星弹；按玩家限流，死亡/登出/切维清理。
 */
public final class WenxingBeamArrowTracker {

    public static final int MAX_ARROWS_PER_PLAYER = 32;

    private static final Map<UUID, WenxingBeamArrow> BY_TARGET = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> TARGET_OF = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> BY_PLAYER = new ConcurrentHashMap<>();

    private WenxingBeamArrowTracker() {
    }

    public static WenxingBeamArrow getOrCreate(Player player, LivingEntity target) {
        if (player == null || target == null || player.level().isClientSide) {
            return null;
        }
        UUID targetId = target.getUUID();
        WenxingBeamArrow existing = BY_TARGET.get(targetId);
        if (existing != null && !existing.isRemoved() && existing.isAlive()
                && existing.level() == target.level()) {
            existing.bindTarget(target);
            return existing;
        }
        if (existing != null) {
            remove(existing);
        }

        Set<UUID> owned = BY_PLAYER.computeIfAbsent(player.getUUID(), k -> ConcurrentHashMap.newKeySet());
        if (owned.size() >= MAX_ARROWS_PER_PLAYER) {
            discardOldest(owned);
        }

        WenxingBeamArrow arrow = new WenxingBeamArrow(
                com.example.attributearch.registry.ModEntityTypes.WENXING_BEAM_ARROW.get(),
                player,
                target.level());
        arrow.setOwner(player);
        arrow.setPos(target.getX(), target.getY() + target.getBbHeight() * 0.45D, target.getZ());
        arrow.bindTarget(target);
        arrow.setTargetUuid(targetId);
        target.level().addFreshEntity(arrow);

        BY_TARGET.put(targetId, arrow);
        TARGET_OF.put(arrow.getUUID(), targetId);
        owned.add(arrow.getUUID());
        return arrow;
    }

    public static UUID getTargetUuid(WenxingBeamArrow arrow) {
        return TARGET_OF.get(arrow.getUUID());
    }

    public static void setTargetUuid(WenxingBeamArrow arrow, UUID targetId) {
        if (targetId == null) {
            TARGET_OF.remove(arrow.getUUID());
        } else {
            TARGET_OF.put(arrow.getUUID(), targetId);
        }
    }

    public static void onTargetRemoved(LivingEntity target) {
        if (target == null) {
            return;
        }
        WenxingBeamArrow arrow = BY_TARGET.remove(target.getUUID());
        if (arrow != null) {
            remove(arrow);
            if (!arrow.isRemoved()) {
                arrow.discard();
            }
        }
    }

    public static void onPlayerGone(Player player) {
        if (player == null) {
            return;
        }
        Set<UUID> owned = BY_PLAYER.remove(player.getUUID());
        if (owned == null) {
            return;
        }
        for (UUID arrowId : owned) {
            UUID targetId = TARGET_OF.remove(arrowId);
            if (targetId != null) {
                WenxingBeamArrow arrow = BY_TARGET.remove(targetId);
                if (arrow != null && !arrow.isRemoved()) {
                    arrow.discard();
                }
            }
        }
    }

    private static void remove(WenxingBeamArrow arrow) {
        UUID arrowId = arrow.getUUID();
        UUID targetId = TARGET_OF.remove(arrowId);
        if (targetId != null) {
            BY_TARGET.remove(targetId, arrow);
        }
        for (Set<UUID> owned : BY_PLAYER.values()) {
            owned.remove(arrowId);
        }
    }

    private static void discardOldest(Set<UUID> owned) {
        Iterator<UUID> it = owned.iterator();
        while (owned.size() >= MAX_ARROWS_PER_PLAYER && it.hasNext()) {
            UUID arrowId = it.next();
            it.remove();
            UUID targetId = TARGET_OF.remove(arrowId);
            if (targetId != null) {
                WenxingBeamArrow arrow = BY_TARGET.remove(targetId);
                if (arrow != null && !arrow.isRemoved()) {
                    arrow.discard();
                }
            }
        }
    }
}
