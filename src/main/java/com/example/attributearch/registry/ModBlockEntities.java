package com.example.attributearch.registry;

import java.util.function.Supplier;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.blockentity.AttributeAltarBlockEntity;
import com.example.attributearch.blockentity.EnchantingAltarBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private ModBlockEntities() {
    }

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AttributeArch.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AttributeAltarBlockEntity>> ATTRIBUTE_ALTAR =
            BLOCK_ENTITIES.register("attribute_altar", () ->
                    BlockEntityType.Builder.of(AttributeAltarBlockEntity::new, ModBlocks.ATTRIBUTE_ALTAR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnchantingAltarBlockEntity>> ENCHANTING_ALTAR =
            BLOCK_ENTITIES.register("enchanting_altar", () ->
                    BlockEntityType.Builder.of(EnchantingAltarBlockEntity::new, ModBlocks.ENCHANTING_ALTAR.get()).build(null));
}
