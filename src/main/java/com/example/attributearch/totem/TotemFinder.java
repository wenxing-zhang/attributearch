package com.example.attributearch.totem;

import com.example.attributearch.compat.curios.CuriosCompat;
import com.example.attributearch.item.WenxingTotemItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class TotemFinder {
    private TotemFinder() {
    }

    public static boolean hasWenxingTotem(Player player) {
        if (player == null) {
            return false;
        }
        if (WenxingTotemItem.isTotem(player.getMainHandItem())
                || WenxingTotemItem.isTotem(player.getOffhandItem())) {
            return true;
        }
        Inventory inv = player.getInventory();
        int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            if (WenxingTotemItem.isTotem(stack)) {
                return true;
            }
        }
        return CuriosCompat.hasTotemInDedicatedSlot(player);
    }
}
