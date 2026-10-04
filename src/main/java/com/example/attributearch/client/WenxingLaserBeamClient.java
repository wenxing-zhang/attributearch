package com.example.attributearch.client;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.item.WenxingBowItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = AttributeArch.MODID, value = Dist.CLIENT)
public final class WenxingLaserBeamClient {

    private static final DustParticleOptions BEAM =
            new DustParticleOptions(new Vector3f(0.55F, 0.15F, 1.0F), 0.45F);

    private WenxingLaserBeamClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        ItemStack stack = player.getUseItem();
        if (!(stack.getItem() instanceof WenxingBowItem)) {
            return;
        }
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        HitResult hit = player.pick(256.0D, 1.0F, false);
        Vec3 end = hit.getType() == HitResult.Type.MISS
                ? start.add(look.scale(256.0D))
                : hit.getLocation();
        if (hit instanceof EntityHitResult entityHit) {
            end = entityHit.getEntity().getBoundingBox().getCenter();
        }
        spawnBeam(mc, start, end);
    }

    private static void spawnBeam(Minecraft mc, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double len = delta.length();
        if (len < 0.01D) {
            return;
        }
        int steps = (int) Math.min(64, Math.ceil(len * 2.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 pos = start.add(delta.scale((double) i / steps));
            mc.level.addParticle(BEAM, pos.x, pos.y, pos.z, 0.0D, 0.0D, 0.0D);
        }
        mc.level.addParticle(BEAM, end.x, end.y, end.z, 0.0D, 0.0D, 0.0D);
    }
}
