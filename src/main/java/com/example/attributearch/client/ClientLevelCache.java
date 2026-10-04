package com.example.attributearch.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

public final class ClientLevelCache {
    private static Map<ResourceLocation, Integer> levels = Collections.emptyMap();

    private ClientLevelCache() {
    }

    public static void setLevels(Map<ResourceLocation, Integer> newLevels) {
        levels = newLevels == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(newLevels));
    }

    public static int getLevel(ResourceLocation attrId) {
        return levels.getOrDefault(attrId, 0);
    }

    public static Map<ResourceLocation, Integer> snapshot() {
        return levels;
    }

    public static void clear() {
        levels = Collections.emptyMap();
    }
}
