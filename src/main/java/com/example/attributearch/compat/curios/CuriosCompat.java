package com.example.attributearch.compat.curios;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

public final class CuriosCompat {
    public static final String SLOT_ID = "wenxing_totem";

    private static final boolean LOADED = ModList.get().isLoaded("curios");

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    public static void init() {
        if (!LOADED) {
            return;
        }
        CuriosCompatImpl.register();
    }

    public static boolean hasTotemInDedicatedSlot(Player player) {
        if (!LOADED || player == null) {
            return false;
        }
        return CuriosCompatImpl.hasTotemInDedicatedSlot(player);
    }
}
