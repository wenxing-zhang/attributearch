package com.example.attributearch.network;

import java.util.HashMap;
import java.util.Map;

import com.example.attributearch.AttributeArch;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncPlayerLevelsPayload(Map<ResourceLocation, Integer> levels) implements CustomPacketPayload {
    public static final Type<SyncPlayerLevelsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AttributeArch.MODID, "sync_levels"));

    private static final int MAX_ENTRIES = 4096;

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerLevelsPayload> STREAM_CODEC =
            StreamCodec.of(SyncPlayerLevelsPayload::encode, SyncPlayerLevelsPayload::decode);

    public SyncPlayerLevelsPayload {
        levels = levels == null ? Map.of() : Map.copyOf(levels);
    }

    private static void encode(RegistryFriendlyByteBuf buf, SyncPlayerLevelsPayload payload) {
        Map<ResourceLocation, Integer> map = payload.levels();
        if (map.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many enhance-levels entries: " + map.size());
        }
        buf.writeVarInt(map.size());
        for (Map.Entry<ResourceLocation, Integer> entry : map.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue() == null ? 0 : entry.getValue());
        }
    }

    private static SyncPlayerLevelsPayload decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid enhance-levels sync size: " + size);
        }
        Map<ResourceLocation, Integer> map = new HashMap<>(Math.max(16, size));
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int level = buf.readVarInt();
            map.put(id, level);
        }
        return new SyncPlayerLevelsPayload(map);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPlayerLevelsPayload payload, IPayloadContext context) {
        if (!context.flow().isClientbound()) {
            return;
        }
        context.enqueueWork(() -> ClientHooks.ON_LEVELS_SYNC.accept(payload.levels()));
    }
}
