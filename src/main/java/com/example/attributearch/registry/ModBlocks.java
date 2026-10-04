package com.example.attributearch.registry;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.block.AttributeAltarBlock;
import com.example.attributearch.block.EnchantingAltarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    private ModBlocks() {
    }

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AttributeArch.MODID);

    public static final DeferredBlock<AttributeAltarBlock> ATTRIBUTE_ALTAR = BLOCKS.register(
            "attribute_altar",
            () -> new AttributeAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK)));

    public static final DeferredBlock<EnchantingAltarBlock> ENCHANTING_ALTAR = BLOCKS.register(
            "enchanting_altar",
            () -> new EnchantingAltarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK)));
}
