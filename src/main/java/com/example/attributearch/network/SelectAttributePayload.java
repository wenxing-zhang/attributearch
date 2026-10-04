package com.example.attributearch.network;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.menu.AttributeAltarMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SelectAttributePayload(ResourceLocation attributeId) implements CustomPacketPayload {
    public static final Type<SelectAttributePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AttributeArch.MODID, "select_attribute"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectAttributePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC,
                    SelectAttributePayload::attributeId,
                    SelectAttributePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectAttributePayload payload, IPayloadContext context) {
        if (context.flow().isClientbound() || payload.attributeId() == null) {
            return;
        }
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!(serverPlayer.containerMenu instanceof AttributeAltarMenu menu)) {
                return;
            }
            if (!menu.stillValid(serverPlayer)) {
                return;
            }

            if (serverPlayer.getAttribute(
                    net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(payload.attributeId())
                            .orElse(null)) == null) {
                return;
            }
            if (!com.example.attributearch.attribute.EnhanceableAttributes.isAllowed(payload.attributeId())) {
                return;
            }
            menu.setSelected(payload.attributeId());
        });
    }
}
