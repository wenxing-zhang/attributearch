package com.example.attributearch.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.example.attributearch.AttributeArch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.loading.FMLPaths;

public final class Whitelist {
    private static final String FILE_NAME = "wenxingtools_whitelist.json";
    private static volatile Set<UUID> cached = new HashSet<>();
    private static volatile boolean loaded;

    private Whitelist() {
    }

    public static boolean contains(UUID uuid) {
        ensureLoaded();
        return cached.contains(uuid);
    }

    public static boolean isAllowed(Player player) {
        if (player == null) {
            return false;
        }
        if (player.hasPermissions(3)) {
            return true;
        }
        return contains(player.getUUID());
    }

    public static int reload() {
        cached = read();
        loaded = true;
        return cached.size();
    }

    private static void ensureLoaded() {
        if (!loaded) {
            reload();
        }
    }

    private static Set<UUID> read() {
        Set<UUID> result = new HashSet<>();
        for (Path path : candidatePaths()) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                String text = Files.readString(path, StandardCharsets.UTF_8);
                JsonElement root = JsonParser.parseString(text);
                if (!(root instanceof JsonArray array)) {
                    continue;
                }
                for (JsonElement element : array) {
                    try {
                        result.add(UUID.fromString(element.getAsString()));
                    } catch (Exception ignored) {

                    }
                }
                AttributeArch.LOGGER.info("Loaded {} whitelist entries from {}", result.size(), path);
                return result;
            } catch (IOException | RuntimeException ex) {
                AttributeArch.LOGGER.warn("Failed to read whitelist at {}", path, ex);
            }
        }
        return result;
    }

    private static Path[] candidatePaths() {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path gameDir = FMLPaths.GAMEDIR.get();
        return new Path[] {
                configDir.resolve(FILE_NAME),
                gameDir.resolve(FILE_NAME),
                Path.of(FILE_NAME)
        };
    }
}
