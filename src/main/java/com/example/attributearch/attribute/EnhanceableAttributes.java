package com.example.attributearch.attribute;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.attributearch.config.ModConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;

public final class EnhanceableAttributes {

    private static List<Holder<Attribute>> registryCache;

    private EnhanceableAttributes() {
    }

    public static void invalidateCache() {
        registryCache = null;
    }

    public static boolean isAllowed(ResourceLocation attrId) {
        if (attrId == null) {
            return false;
        }
        String key = attrId.toString();
        if (ModConfig.BLACKLISTED_ATTRIBUTES.get().contains(key)) {
            return false;
        }
        if (ModConfig.USE_ALL_PLAYER_ATTRIBUTES.get()) {
            return true;
        }
        return ModConfig.ENABLED_ATTRIBUTES.get().contains(key);
    }

    private static List<Holder<Attribute>> registryHolders() {
        List<Holder<Attribute>> cache = registryCache;
        if (cache == null) {
            cache = List.copyOf(BuiltInRegistries.ATTRIBUTE.holders().toList());
            registryCache = cache;
        }
        return cache;
    }

    public static List<Holder<Attribute>> list(Player player) {
        Set<String> blacklist = new HashSet<>(ModConfig.BLACKLISTED_ATTRIBUTES.get());
        List<Holder<Attribute>> result = new ArrayList<>();

        if (ModConfig.USE_ALL_PLAYER_ATTRIBUTES.get()) {
            for (Holder<Attribute> holder : registryHolders()) {
                if (blacklist.contains(holderKey(holder))) {
                    continue;
                }
                if (player.getAttribute(holder) != null) {
                    result.add(holder);
                }
            }
        } else {
            Set<String> enabled = new HashSet<>(ModConfig.ENABLED_ATTRIBUTES.get());
            for (String raw : enabled) {
                if (blacklist.contains(raw)) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id == null) {
                    continue;
                }
                BuiltInRegistries.ATTRIBUTE.getHolder(id).ifPresent(holder -> {
                    if (player.getAttribute(holder) != null) {
                        result.add(holder);
                    }
                });
            }
        }

        result.sort(Comparator.comparing(EnhanceableAttributes::displayName)
                .thenComparing(EnhanceableAttributes::holderKey));
        return result;
    }

    public static String displayName(Holder<Attribute> holder) {
        Component name = Component.translatable(holder.value().getDescriptionId());
        String text = name.getString();
        if (text == null || text.isBlank() || text.equals(holder.value().getDescriptionId())) {
            return prettify(holderKey(holder));
        }
        return text;
    }

    public static String prettify(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        String[] parts = path.replace('/', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.isEmpty() ? id : sb.toString();
    }

    public static String holderKey(Holder<Attribute> holder) {
        ResourceLocation key = BuiltInRegistries.ATTRIBUTE.getKey(holder.value());
        return key == null ? holder.value().getDescriptionId() : key.toString();
    }

    public static Holder<Attribute> resolve(Player player, String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String trimmed = input.trim();

        ResourceLocation asId = ResourceLocation.tryParse(trimmed);
        if (asId != null) {
            return BuiltInRegistries.ATTRIBUTE.getHolder(asId).orElse(null);
        }

        for (Holder<Attribute> holder : list(player)) {
            if (displayName(holder).equalsIgnoreCase(trimmed)
                    || holderKey(holder).equalsIgnoreCase(trimmed)) {
                return holder;
            }
        }
        return null;
    }
}
