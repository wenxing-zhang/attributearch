package com.example.attributearch.network;

import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;

public final class ClientHooks {
    private ClientHooks() {
    }

    public static volatile Consumer<Map<ResourceLocation, Integer>> ON_LEVELS_SYNC = levels -> {
    };

    public static volatile Runnable ON_LOGGED_OUT = () -> {
    };
}
