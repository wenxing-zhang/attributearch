package com.example.attributearch.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;

public final class ArrowResolver {

    public static final double VANILLA_ARROW_BONUS = 2.0D;

    private ArrowResolver() {
    }

    public record ResolvedArrow(double damageBonus, List<MobEffectInstance> effects, boolean spectral,
                                String displayName) {

        public static ResolvedArrow vanilla() {
            return new ResolvedArrow(VANILLA_ARROW_BONUS, List.of(), false, "item.minecraft.arrow");
        }
    }

    public static ItemStack findArrow(Player player) {
        Inventory inv = player.getInventory();
        ItemStack held = player.getMainHandItem();
        if (isNormalArrow(held)) {
            return held;
        }
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isNormalArrow(stack)) {
                return stack;
            }
        }
        for (int i = Inventory.getSelectionSize(); i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (isNormalArrow(stack)) {
                return stack;
            }
        }
        ItemStack off = player.getOffhandItem();
        if (isNormalArrow(off)) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    public static boolean isNormalArrow(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.TIPPED_ARROW)
                || stack.getItem() instanceof net.minecraft.world.item.ArrowItem);
    }

    public static ResolvedArrow resolve(Player player) {
        ItemStack stack = findArrow(player);
        if (stack.isEmpty()) {
            return ResolvedArrow.vanilla();
        }
        return fromStack(stack);
    }

    public static ResolvedArrow fromStack(ItemStack stack) {
        double bonus = VANILLA_ARROW_BONUS;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            var tag = custom.copyTag();
            if (tag.contains("damage")) {
                double d = tag.getDouble("damage");
                if (d > 0) {
                    bonus = d;
                }
            }
        }
        boolean spectral = stack.is(Items.SPECTRAL_ARROW);
        List<MobEffectInstance> effects = new ArrayList<>();
        if (stack.is(Items.TIPPED_ARROW)) {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents != null) {
                contents.getAllEffects().forEach(effects::add);
            }
        }
        String name = stack.getItem().getDescriptionId();
        return new ResolvedArrow(bonus, List.copyOf(effects), spectral, name);
    }

    public static List<MobEffectInstance> withSpectral(List<MobEffectInstance> base, boolean spectral) {
        if (!spectral) {
            return base;
        }
        List<MobEffectInstance> list = new ArrayList<>(base);
        list.add(new MobEffectInstance(MobEffects.GLOWING, 200, 0, true, false));
        return list;
    }
}
