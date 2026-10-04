package com.example.attributearch.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 文星弹：每目标复用的归属箭，不造成二次伤害，仅作伤害源归因与插箭表现。
 */
public class WenxingBeamArrow extends AbstractArrow {

    private static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(WenxingBeamArrow.class, EntityDataSerializers.INT);

    public WenxingBeamArrow(EntityType<? extends WenxingBeamArrow> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setNoPhysics(true);
        this.pickup = Pickup.DISALLOWED;
    }

    public WenxingBeamArrow(EntityType<? extends WenxingBeamArrow> type, LivingEntity shooter, Level level) {
        super(type, shooter, level, ItemStack.EMPTY, null);
        this.setNoGravity(true);
        this.setNoPhysics(true);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TARGET_ID, -1);
    }

    public void bindTarget(LivingEntity target) {
        this.entityData.set(DATA_TARGET_ID, target.getId());
        stickTo(target);
    }

    public UUID getTargetUuid() {
        return WenxingBeamArrowTracker.getTargetUuid(this);
    }

    public void setTargetUuid(UUID uuid) {
        WenxingBeamArrowTracker.setTargetUuid(this, uuid);
    }

    private void stickTo(Entity target) {
        this.setPos(target.getX(), target.getY() + target.getBbHeight() * 0.45D, target.getZ());
        this.setDeltaMovement(Vec3.ZERO);
        this.setNoGravity(true);
        this.setOnGround(false);
    }

    @Override
    public void tick() {
        if (this.level().isClientSide) {
            super.tick();
            return;
        }
        Entity target = this.level().getEntity(this.entityData.get(DATA_TARGET_ID));
        if (target instanceof LivingEntity living && living.isAlive()) {
            stickTo(living);
            return;
        }
        this.discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID target = WenxingBeamArrowTracker.getTargetUuid(this);
        if (target != null) {
            tag.putUUID("TargetUuid", target);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("TargetUuid")) {
            WenxingBeamArrowTracker.setTargetUuid(this, tag.getUUID("TargetUuid"));
        }
    }
}
