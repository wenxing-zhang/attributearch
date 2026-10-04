package com.example.attributearch.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.enchant.EnchantingAltarHelper;
import com.example.attributearch.menu.EnchantingAltarMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ConfirmEnchantPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<ConfirmEnchantPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(AttributeArch.MODID, "confirm_enchant"));

    private static final int MAX_ENTRIES = 256;

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfirmEnchantPayload> STREAM_CODEC =
            StreamCodec.of(ConfirmEnchantPayload::encode, ConfirmEnchantPayload::decode);

    public record Entry(ResourceLocation enchantmentId, int level) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
                StreamCodec.composite(
                        ResourceLocation.STREAM_CODEC,
                        Entry::enchantmentId,
                        ByteBufCodecs.VAR_INT,
                        Entry::level,
                        Entry::new);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buf, ConfirmEnchantPayload payload) {
        List<Entry> list = payload.entries();
        if (list.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many enchantment entries: " + list.size());
        }
        buf.writeVarInt(list.size());
        for (Entry entry : list) {
            Entry.STREAM_CODEC.encode(buf, entry);
        }
    }

    private static ConfirmEnchantPayload decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid enchantment entries size: " + size);
        }
        List<Entry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(Entry.STREAM_CODEC.decode(buf));
        }
        return new ConfirmEnchantPayload(list);
    }

    public static void handle(ConfirmEnchantPayload payload, IPayloadContext context) {
        if (context.flow().isClientbound()) {
            return;
        }
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (!(serverPlayer.containerMenu instanceof EnchantingAltarMenu menu)) {
                return;
            }
            if (!menu.stillValid(serverPlayer)) {
                serverPlayer.displayClientMessage(
                        Component.translatable("attributearch.gui.enchant_out_of_range"), true);
                return;
            }
            Map<ResourceLocation, Integer> map = new LinkedHashMap<>();
            for (Entry entry : payload.entries()) {

                if (entry.enchantmentId() != null && entry.level() >= 0) {
                    map.put(entry.enchantmentId(), entry.level());
                }
            }
            if (map.isEmpty()) {
                serverPlayer.displayClientMessage(
                        Component.translatable("attributearch.gui.enchant_select_first"), true);
                return;
            }

            ItemStack stack = menu.getInputItem();
            if (stack.isEmpty()) {
                serverPlayer.displayClientMessage(
                        Component.translatable("attributearch.gui.enchant_need_item"), true);
                return;
            }

            if (!EnchantingAltarHelper.isSelectionsDirty(serverPlayer.level(), stack, map)) {
                serverPlayer.displayClientMessage(
                        Component.translatable("attributearch.gui.enchant_no_change"), true);
                return;
            }
            int cost = EnchantingAltarHelper.effectiveCost(serverPlayer.level(), stack, map);
            if (cost > 0 && !serverPlayer.getAbilities().instabuild
                    && serverPlayer.experienceLevel < cost) {
                serverPlayer.displayClientMessage(
                        Component.translatable("attributearch.gui.need_xp", cost), true);
                return;
            }
            ItemStack result = EnchantingAltarHelper.computeResult(serverPlayer, stack, map);
            if (!result.isEmpty()) {

                if (cost > 0 && !serverPlayer.getAbilities().instabuild) {
                    serverPlayer.giveExperienceLevels(-cost);
                }
                menu.getInputContainer().setItem(EnchantingAltarMenu.INPUT_SLOT, result);
                menu.getInputContainer().setChanged();
                menu.onInputChanged();

                serverPlayer.level().playSound(null, menu.getPos(),
                        SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 1.0F,
                        serverPlayer.level().random.nextFloat() * 0.1F + 0.9F);
            } else {
                serverPlayer.displayClientMessage(
                        Component.translatable("attributearch.gui.enchant_failed"), true);
            }
        });
    }

    public static ConfirmEnchantPayload fromMap(Map<ResourceLocation, Integer> map) {

        List<Entry> list = new ArrayList<>(Math.min(Math.max(map.size(), 0), MAX_ENTRIES));
        map.forEach((id, level) -> {
            if (level != null && level >= 0 && list.size() < MAX_ENTRIES) {
                list.add(new Entry(id, level));
            }
        });
        return new ConfirmEnchantPayload(list);
    }
}
