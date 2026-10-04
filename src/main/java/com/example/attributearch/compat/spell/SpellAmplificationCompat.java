package com.example.attributearch.compat.spell;

import com.example.attributearch.compat.goety.GoetySpellPotencyCompat;
import com.example.attributearch.compat.irons.IronsSpellPowerCompat;
import net.minecraft.world.entity.player.Player;

public final class SpellAmplificationCompat {
    private SpellAmplificationCompat() {
    }

    public static void init() {
        GoetySpellPotencyCompat.init();
        IronsSpellPowerCompat.init();
    }

    public static void sync(Player player) {
        GoetySpellPotencyCompat.sync(player);
        IronsSpellPowerCompat.sync(player);
    }

    public static void clear(Player player) {
        GoetySpellPotencyCompat.clear(player);
        IronsSpellPowerCompat.clear(player);
    }
}
