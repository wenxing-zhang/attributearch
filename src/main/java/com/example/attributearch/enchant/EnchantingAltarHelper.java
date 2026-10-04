package com.example.attributearch.enchant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.core.Registry;

public final class EnchantingAltarHelper {
    private EnchantingAltarHelper() {
    }

    public static boolean isBook(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK));
    }

    public static boolean canInsert(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (isBook(stack)) {
            return true;
        }
        return stack.isEnchantable() || stack.isEnchanted() || stack.isDamageableItem();
    }

    public static boolean canApply(ItemStack stack, Holder<Enchantment> holder) {
        if (stack == null || stack.isEmpty() || holder == null) {
            return false;
        }
        if (isBook(stack)) {
            return true;
        }
        try {
            if (stack.supportsEnchantment(holder)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            return holder.value().canEnchant(stack);
        } catch (Throwable t) {
            return true;
        }
    }

    public static int costFor(Map<ResourceLocation, Integer> selections) {
        int cost = 0;
        if (selections == null) {
            return 0;
        }
        for (Integer lv : selections.values()) {
            if (lv != null && lv > 0) {
                cost += lv;
            }
        }
        return cost;
    }

    public static int costForResolved(Map<Holder<Enchantment>, Integer> resolved) {
        int cost = 0;
        if (resolved == null) {
            return 0;
        }
        for (Integer lv : resolved.values()) {
            if (lv != null && lv > 0) {
                cost += lv;
            }
        }
        return cost;
    }

    public static int effectiveCost(Level level, ItemStack stack, Map<ResourceLocation, Integer> selections) {
        if (selections == null || selections.isEmpty()) {
            return 0;
        }
        Map<Holder<Enchantment>, Integer> resolved = resolveSelections(level, stack, selections);
        ItemEnchantments existing = getAppliedEnchantments(stack);
        int cost = 0;
        for (Map.Entry<Holder<Enchantment>, Integer> e : resolved.entrySet()) {
            Integer afterLv = e.getValue();
            if (afterLv == null || afterLv <= 0) {
                continue;
            }
            int beforeLv = existing.getLevel(e.getKey());
            if (beforeLv <= 0 || afterLv > beforeLv) {
                cost += afterLv;
            }
        }
        return cost;
    }

    private static Registry<Enchantment> registry(Level level) {
        return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }

    private static List<Holder<Enchantment>> discoverableCache = List.of();
    private static int discoverableCacheSize = -1;

    public static void invalidateCache() {
        discoverableCache = List.of();
        discoverableCacheSize = -1;
    }

    public static boolean isDiscoverable(Holder<Enchantment> holder) {
        if (holder.is(EnchantmentTags.IN_ENCHANTING_TABLE)
                || holder.is(EnchantmentTags.TREASURE)
                || holder.is(EnchantmentTags.CURSE)
                || holder.is(EnchantmentTags.TRADEABLE)
                || holder.is(EnchantmentTags.ON_RANDOM_LOOT)
                || holder.is(EnchantmentTags.ON_TRADED_EQUIPMENT)
                || holder.is(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT)) {
            return true;
        }

        Boolean defined = readDefinitionDiscoverable(holder.value());
        return defined == null || defined;
    }

    private static Boolean readDefinitionDiscoverable(Enchantment ench) {
        try {
            for (Class<?> c = ench.getClass(); c != null; c = c.getSuperclass()) {
                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (f.getType() != boolean.class) {
                        continue;
                    }
                    String n = f.getName().toLowerCase(java.util.Locale.ROOT);
                    if (n.contains("discover")) {
                        f.setAccessible(true);
                        return f.getBoolean(ench);
                    }
                }

                for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType() == boolean.class) {
                        String n = m.getName().toLowerCase(java.util.Locale.ROOT);
                        if (n.equals("discoverable") || n.equals("isdiscoverable") || n.equals("discoverableboolean")) {
                            m.setAccessible(true);
                            return (Boolean) m.invoke(ench);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {

        }
        return null;
    }

    private static List<Holder<Enchantment>> discoverable(Level level) {
        List<Holder<Enchantment>> cached = discoverableCache;
        if (discoverableCacheSize >= 0 && cached != null) {
            return cached;
        }
        Registry<Enchantment> reg = registry(level);

        List<Holder<Enchantment>> list = new ArrayList<>();
        for (Holder<Enchantment> holder : reg.holders().toList()) {
            if (isDiscoverable(holder)) {
                list.add(holder);
            }
        }
        discoverableCache = List.copyOf(list);
        discoverableCacheSize = reg.size();
        return discoverableCache;
    }

    public static List<Holder<Enchantment>> listAvailable(Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) {
            return List.of();
        }
        boolean book = isBook(stack);
        List<Holder<Enchantment>> result = new ArrayList<>();
        for (Holder<Enchantment> holder : discoverable(level)) {
            if (!book) {
                if (!canApply(stack, holder)) {
                    continue;
                }
            }
            result.add(holder);
        }
        ItemEnchantments existing = getAppliedEnchantments(stack);

        Map<Holder<Enchantment>, String> nameKeys = new LinkedHashMap<>();
        for (Holder<Enchantment> holder : result) {
            nameKeys.put(holder, displayName(holder));
        }
        result.sort(Comparator
                .comparing((Holder<Enchantment> h) -> existing.getLevel(h) <= 0)
                .thenComparing((Holder<Enchantment> h) -> h.is(EnchantmentTags.CURSE))
                .thenComparing(Comparator.comparingInt((Holder<Enchantment> h) -> h.value().getWeight()).reversed())
                .thenComparing(nameKeys::get));
        return result;
    }

    public static ItemEnchantments getAppliedEnchantments(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemEnchantments.EMPTY;
        }
        return EnchantmentHelper.getEnchantmentsForCrafting(stack);
    }

    public static boolean isConflicting(Holder<Enchantment> a, Holder<Enchantment> b) {
        return !Enchantment.areCompatible(a, b);
    }

    public static Map<Holder<Enchantment>, Integer> resolveSelections(
            Level level, ItemStack stack, Map<ResourceLocation, Integer> selections) {
        Map<Holder<Enchantment>, Integer> resolved = new LinkedHashMap<>();
        if (level == null || selections == null || selections.isEmpty()) {
            return resolved;
        }
        boolean book = isBook(stack);
        Registry<Enchantment> reg = registry(level);
        for (Map.Entry<ResourceLocation, Integer> entry : selections.entrySet()) {
            int levelVal = entry.getValue() == null ? 0 : entry.getValue();
            if (levelVal < 0) {
                continue;
            }
            Optional<Holder.Reference<Enchantment>> holderOpt = reg.getHolder(entry.getKey());
            if (holderOpt.isEmpty()) {
                continue;
            }
            Holder<Enchantment> holder = holderOpt.get();
            if (levelVal == 0) {

                resolved.put(holder, 0);
                continue;
            }
            if (!book && !stack.isEmpty() && !canApply(stack, holder)) {
                continue;
            }
            int min = holder.value().getMinLevel();
            int max = getMaxLevel(holder);
            resolved.put(holder, Mth.clamp(levelVal, min, max));
        }
        return resolved;
    }

    public static List<Holder<Enchantment>> findConflicts(
            Level level,
            ItemStack stack,
            Holder<Enchantment> candidate,
            Map<Holder<Enchantment>, Integer> resolvedSelections) {
        List<Holder<Enchantment>> conflicts = new ArrayList<>();
        if (level == null || candidate == null || stack.isEmpty()) {
            return conflicts;
        }

        for (Map.Entry<Holder<Enchantment>, Integer> entry : getAppliedEnchantments(stack).entrySet()) {
            if (entry.getKey().is(candidate)) {
                continue;
            }
            Integer override = resolvedSelections.get(entry.getKey());
            if (override != null) {

                if (override <= 0 || isConflicting(candidate, entry.getKey())) {

                    if (override <= 0) {
                        continue;
                    }
                }
            }
            if (resolvedSelections.containsKey(entry.getKey())) {

                continue;
            }
            if (isConflicting(candidate, entry.getKey())) {
                conflicts.add(entry.getKey());
            }
        }
        for (Map.Entry<Holder<Enchantment>, Integer> entry : resolvedSelections.entrySet()) {
            if (entry.getKey().is(candidate)) {
                continue;
            }
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            if (isConflicting(candidate, entry.getKey())) {
                conflicts.add(entry.getKey());
            }
        }
        return conflicts;
    }

    public static boolean hasConflict(
            Level level, ItemStack stack, ResourceLocation candidateId, int candidateLevel,
            Map<ResourceLocation, Integer> selections) {
        if (candidateLevel <= 0 || level == null || candidateId == null) {
            return false;
        }
        Holder<Enchantment> candidate = resolve(level, candidateId);
        if (candidate == null) {
            return true;
        }
        return !findConflicts(level, stack, candidate, resolveSelections(level, stack, selections)).isEmpty();
    }

    public static ItemStack getEnchantedItemStack(ItemStack stack, boolean isEnchanted) {
        if (stack.is(Items.ENCHANTED_BOOK) && !isEnchanted) {
            ItemStack plain = stack.transmuteCopy(Items.BOOK);
            plain.remove(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
            return plain;
        }
        if (stack.is(Items.BOOK) && isEnchanted) {
            return stack.transmuteCopy(Items.ENCHANTED_BOOK);
        }
        return stack.copy();
    }

    public static ItemStack setNewEnchantments(ItemStack stack, Map<Holder<Enchantment>, Integer> newEnchantments) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemEnchantments existing = getAppliedEnchantments(stack);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(existing);
        if (newEnchantments != null) {
            newEnchantments.forEach(mutable::set);
        }
        ItemEnchantments target = mutable.toImmutable();
        ItemStack result = getEnchantedItemStack(stack, !target.isEmpty());
        EnchantmentHelper.setEnchantments(result, target);
        return result;
    }

    public static ItemStack computeResult(Player player, ItemStack original, Map<ResourceLocation, Integer> selections) {

        if (original.isEmpty() || selections == null || selections.isEmpty()) {
            return ItemStack.EMPTY;
        }

        boolean book = isBook(original);
        Map<Holder<Enchantment>, Integer> resolved = resolveSelections(player.level(), original, selections);
        if (resolved.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemEnchantments existing = getAppliedEnchantments(original);
        ItemEnchantments target = mergeSelections(original, resolved);
        if (target.equals(existing)) {
            return ItemStack.EMPTY;
        }

        int cost = effectiveCost(player.level(), original, selections);
        if (!canAfford(player, cost)) {
            return ItemStack.EMPTY;
        }

        List<Holder<Enchantment>> applied = new ArrayList<>();
        for (Map.Entry<Holder<Enchantment>, Integer> entry : resolved.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            int levelVal = entry.getValue() == null ? 0 : entry.getValue();
            if (levelVal <= 0) {
                continue;
            }
            if (!isDiscoverable(holder)) {
                return ItemStack.EMPTY;
            }
            if (!book && !canApply(original, holder)) {
                return ItemStack.EMPTY;
            }
            for (Holder<Enchantment> other : applied) {
                if (!Enchantment.areCompatible(holder, other)) {
                    return ItemStack.EMPTY;
                }
            }
            for (Object2IntMap.Entry<Holder<Enchantment>> existingEntry : existing.entrySet()) {
                Holder<Enchantment> other = existingEntry.getKey();
                Integer override = resolved.get(other);
                if (override != null && override <= 0) {
                    continue;
                }
                if (resolved.containsKey(other) || other.is(holder)) {
                    continue;
                }
                if (!Enchantment.areCompatible(holder, other)) {
                    return ItemStack.EMPTY;
                }
            }
            applied.add(holder);
        }

        return setNewEnchantments(original, resolved);
    }

    public static boolean tryRepair(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0) {
            return false;
        }
        stack.setDamageValue(0);
        return true;
    }

    public static boolean isSelectionsDirty(
            Level level, ItemStack stack, Map<ResourceLocation, Integer> selections) {
        if (stack.isEmpty() || selections == null || selections.isEmpty()) {
            return false;
        }
        ItemEnchantments original = getAppliedEnchantments(stack);
        ItemEnchantments after = mergeSelections(stack, resolveSelections(level, stack, selections));
        return !after.equals(original);
    }

    public static String displayName(Holder<Enchantment> holder, int level) {
        return Enchantment.getFullname(holder, level).getString();
    }

    public static String displayName(Holder<Enchantment> holder) {
        return displayName(holder, 1);
    }

    @Nullable
    public static ResourceLocation idOf(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(net.minecraft.resources.ResourceKey::location).orElse(null);
    }

    @Nullable
    public static Holder<Enchantment> resolve(Level level, ResourceLocation id) {
        if (level == null || id == null) {
            return null;
        }
        return registry(level).getHolder(id).orElse(null);
    }

    public static int getMaxLevel(Holder<Enchantment> holder) {
        return holder.value().getMaxLevel();
    }

    public static boolean isTreasure(Holder<Enchantment> holder) {
        return holder.is(EnchantmentTags.TREASURE);
    }

    public static boolean isCurse(Holder<Enchantment> holder) {
        return holder.is(EnchantmentTags.CURSE);
    }

    public static boolean canAfford(Player player, int cost) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        return player.experienceLevel >= cost;
    }

    public static ItemEnchantments mergeSelections(ItemStack stack, Map<Holder<Enchantment>, Integer> resolvedSelections) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(getAppliedEnchantments(stack));
        if (resolvedSelections != null) {
            resolvedSelections.forEach(mutable::set);
        }
        return mutable.toImmutable();
    }
}
