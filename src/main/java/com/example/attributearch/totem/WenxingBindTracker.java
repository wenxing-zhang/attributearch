package com.example.attributearch.totem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WenxingBindTracker {
    private WenxingBindTracker() {
    }

    private static final class BindState {
        int remainTicks;
        double x, y, z;
        float yRot, xRot;
        LivingEntity entity;

        BindState(LivingEntity entity, int ticks) {
            this.entity = entity;
            this.remainTicks = ticks;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
            this.yRot = entity.getYRot();
            this.xRot = entity.getXRot();
        }

        void refreshLock(LivingEntity entity, int ticks) {
            this.entity = entity;
            this.remainTicks = ticks;
            this.x = entity.getX();
            this.y = entity.getY();
            this.z = entity.getZ();
            this.yRot = entity.getYRot();
            this.xRot = entity.getXRot();
        }
    }

    private static final class FlightState {
        final boolean mayfly;
        final boolean flying;

        FlightState(boolean mayfly, boolean flying) {
            this.mayfly = mayfly;
            this.flying = flying;
        }
    }

    private static final Map<UUID, BindState> BOUND = new ConcurrentHashMap<>();
    private static final Map<UUID, FlightState> PREV_FLIGHT = new ConcurrentHashMap<>();

    public static void apply(LivingEntity entity, int ticks) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }

        BindState existing = BOUND.get(entity.getUUID());
        if (existing != null) {
            existing.refreshLock(entity, ticks);
            return;
        }
        BOUND.put(entity.getUUID(), new BindState(entity, ticks));
    }

    public static void clear(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        UUID id = entity.getUUID();
        BOUND.remove(id);
        restoreFlight(entity, id);
    }

    public static boolean isBound(LivingEntity entity) {
        return entity != null && BOUND.containsKey(entity.getUUID());
    }

    public static void tickAll() {
        if (BOUND.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, BindState> entry : BOUND.entrySet()) {
            BindState state = entry.getValue();
            LivingEntity entity = state.entity;
            if (entity == null || entity.isRemoved() || entity.level().isClientSide) {
                if (entity != null) {
                    clear(entity);
                } else {
                    BOUND.remove(entry.getKey());
                    PREV_FLIGHT.remove(entry.getKey());
                }
                continue;
            }
            if (--state.remainTicks <= 0) {
                clear(entity);
                continue;
            }
            lock(entity, state);
        }
    }

    public static void lock(LivingEntity entity, BindState state) {
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        entity.fallDistance = 0.0F;
        entity.setSprinting(false);

        if (entity instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.teleport(state.x, state.y, state.z, state.yRot, state.xRot);
        } else {
            entity.setPos(state.x, state.y, state.z);
            entity.setYRot(state.yRot);
            entity.setXRot(state.xRot);
        }
        entity.yBodyRot = state.yRot;
        entity.yHeadRot = state.yRot;
    }

    public static void freezeFlags(LivingEntity entity) {
        BindState state = BOUND.get(entity.getUUID());
        if (state != null) {
            lock(entity, state);
        }
        if (entity instanceof Player player) {
            FlightState prev = PREV_FLIGHT.get(player.getUUID());
            if (prev == null) {
                PREV_FLIGHT.put(player.getUUID(),
                        new FlightState(player.getAbilities().mayfly, player.getAbilities().flying));
            }
            boolean changed = player.getAbilities().flying || player.getAbilities().mayfly;
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            if (changed && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.onUpdateAbilities();
            }
        }
    }

    private static void restoreFlight(LivingEntity entity, UUID id) {
        FlightState prev = PREV_FLIGHT.remove(id);
        if (entity instanceof Player player && prev != null) {
            player.getAbilities().mayfly = prev.mayfly;
            player.getAbilities().flying = prev.flying;
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.onUpdateAbilities();
            }
        }
    }

    public static boolean isTrainingDummy(Entity entity) {
        if (entity instanceof ArmorStand) {
            return true;
        }
        String path = BuiltInRegistries.ENTITY_TYPE
                .getKey(entity.getType()).getPath().toLowerCase();

        return path.equals("dummy")
                || path.equals("target_dummy")
                || path.equals("training_dummy")
                || path.endsWith("_dummy")
                || path.contains("targetdummy");
    }

    private static final java.lang.reflect.Field EFFECTS_DIRTY = resolveEffectsDirty();

    private static java.lang.reflect.Field resolveEffectsDirty() {
        try {
            java.lang.reflect.Field field = LivingEntity.class.getDeclaredField("effectsDirty");
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void forceEffect(LivingEntity entity, net.minecraft.world.effect.MobEffectInstance instance) {
        if (entity == null || entity.level().isClientSide || instance == null) {
            return;
        }
        if (entity.addEffect(instance)) {
            return;
        }
        try {
            entity.getActiveEffectsMap().put(instance.getEffect(), instance);
            if (EFFECTS_DIRTY != null) {
                EFFECTS_DIRTY.setBoolean(entity, true);
            }
        } catch (Throwable ignored) {
        }
    }
}
