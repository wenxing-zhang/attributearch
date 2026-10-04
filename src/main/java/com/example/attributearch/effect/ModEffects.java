package com.example.attributearch.effect;

import com.example.attributearch.AttributeArch;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    private ModEffects() {
    }

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, AttributeArch.MODID);

    public static final DeferredHolder<MobEffect, WenxingShieldEffect> WENXING_SHIELD =
            MOB_EFFECTS.register("wenxing_shield",
                    () -> new WenxingShieldEffect(MobEffectCategory.BENEFICIAL, 0x9B59B6));

    public static final DeferredHolder<MobEffect, WenxingBindEffect> WENXING_BIND =
            MOB_EFFECTS.register("wenxing_bind",
                    () -> new WenxingBindEffect(MobEffectCategory.HARMFUL, 0x6A0DAD));
}
