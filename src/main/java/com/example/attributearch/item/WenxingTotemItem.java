package com.example.attributearch.item;

import com.example.attributearch.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WenxingTotemItem extends Item {

    public WenxingTotemItem() {
        super(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE)
                .fireResistant());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.attributearch.wenxing_totem.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    public static boolean isTotem(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ModItems.WENXING_TOTEM.get());
    }
}
