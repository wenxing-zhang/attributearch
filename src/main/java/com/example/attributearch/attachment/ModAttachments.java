package com.example.attributearch.attachment;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.example.attributearch.AttributeArch;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private ModAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AttributeArch.MODID);

    private static final class EnhanceLevelsSerializer
            implements IAttachmentSerializer<Tag, Map<ResourceLocation, Integer>> {

        @Override
        public Map<ResourceLocation, Integer> read(IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
            Map<ResourceLocation, Integer> map = new HashMap<>();
            try {
                if (tag instanceof ListTag list) {
                    readList(list, map);
                } else if (tag instanceof CompoundTag compound) {
                    readLegacyCompound(compound, map);
                }
            } catch (Exception ex) {
                AttributeArch.LOGGER.warn("Discarding unreadable enhance-levels attachment", ex);
                return new HashMap<>();
            }
            return map;
        }

        private static void readList(ListTag list, Map<ResourceLocation, Integer> map) {
            for (int i = 0; i < list.size(); i++) {
                Tag raw = list.get(i);
                if (!(raw instanceof CompoundTag entry)) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
                if (id != null) {
                    map.put(id, entry.getInt("level"));
                }
            }
        }

        private static void readLegacyCompound(CompoundTag compound, Map<ResourceLocation, Integer> map) {
            for (String key : compound.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null) {
                    continue;
                }
                Tag value = compound.get(key);
                if (value == null) {
                    continue;
                }
                map.put(id, compound.getInt(key));
            }
        }

        @Override
        public Tag write(Map<ResourceLocation, Integer> attachment, HolderLookup.Provider provider) {
            ListTag list = new ListTag();
            try {
                if (attachment == null) {
                    return list;
                }
                attachment.forEach((id, level) -> {
                    if (id == null || level == null) {
                        return;
                    }
                    CompoundTag entry = new CompoundTag();
                    entry.putString("id", id.toString());
                    entry.putInt("level", level);
                    list.add(entry);
                });
            } catch (Exception ex) {
                AttributeArch.LOGGER.warn("Failed to write enhance-levels attachment", ex);
                return new ListTag();
            }
            return list;
        }
    }

    public static final Supplier<AttachmentType<Map<ResourceLocation, Integer>>> ENHANCE_LEVELS =
            ATTACHMENT_TYPES.register("enhance_levels", () -> {
                Supplier<Map<ResourceLocation, Integer>> defaults = HashMap::new;

                return AttachmentType.builder(defaults)
                        .serialize(new EnhanceLevelsSerializer())
                        .build();
            });
}
