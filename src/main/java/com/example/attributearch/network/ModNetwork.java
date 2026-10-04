package com.example.attributearch.network;

import com.example.attributearch.AttributeArch;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                SelectAttributePayload.TYPE,
                SelectAttributePayload.STREAM_CODEC,
                SelectAttributePayload::handle);
        registrar.playToClient(
                SyncPlayerLevelsPayload.TYPE,
                SyncPlayerLevelsPayload.STREAM_CODEC,
                SyncPlayerLevelsPayload::handle);
        registrar.playToServer(
                ConfirmEnchantPayload.TYPE,
                ConfirmEnchantPayload.STREAM_CODEC,
                ConfirmEnchantPayload::handle);
        AttributeArch.LOGGER.debug("Registered AttributeArch network payloads");
    }
}
