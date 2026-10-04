package com.example.attributearch.registry;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.entity.WenxingBeamArrow;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModEntityTypes {
    private ModEntityTypes() {
    }

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AttributeArch.MODID);

    public static final Supplier<EntityType<WenxingBeamArrow>> WENXING_BEAM_ARROW =
            ENTITY_TYPES.register("wenxing_beam_arrow",
                    () -> EntityType.Builder.<WenxingBeamArrow>of(WenxingBeamArrow::new, MobCategory.MISC)
                            .sized(0.2F, 0.2F)
                            .clientTrackingRange(6)
                            .updateInterval(20)
                            .fireImmune()
                            .build("wenxing_beam_arrow"));
}
