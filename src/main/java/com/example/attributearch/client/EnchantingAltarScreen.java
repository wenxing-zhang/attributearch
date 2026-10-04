package com.example.attributearch.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.enchant.EnchantPinyin;
import com.example.attributearch.enchant.EnchantingAltarHelper;
import com.example.attributearch.menu.EnchantingAltarMenu;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class EnchantingAltarScreen extends AbstractContainerScreen<EnchantingAltarMenu> {
    public static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AttributeArch.MODID, "textures/gui/enchanting_altar.png");

    private static final int IMAGE_W = 220;
    private static final int IMAGE_H = 185;

    private static final int LIST_X = 30;
    private static final int LIST_Y = 18;
    private static final int LIST_W = 160;

    private static final int SRC_ROW_H = 18;

    private static final int ROW_H = 14;

    private static final int LIST_WINDOW_H = 70;
    private static final int MAX_VISIBLE = 5;
    private static final int VISIBLE_H = MAX_VISIBLE * ROW_H;
    private static final int SCROLLBAR_OFFSET = 8;
    private static final int SCROLLBAR_W = 12;
    private static final int SCROLLBAR_H = 15;

    private static final int SCROLL_LINES = 2;

    private final Player player;

    private final java.util.LinkedHashMap<ResourceLocation, Integer> clientSelections = new java.util.LinkedHashMap<>();
    private int lastInputFingerprint;
    private List<Holder<Enchantment>> available = List.of();
    private List<Holder<Enchantment>> filtered = List.of();
    private int scrollOffset;
    private boolean ignoreTextInput;
    private boolean draggingScrollbar;

    private boolean listDirty = true;
    private boolean tooltipDirty = true;
    private int lastSelectionsModCount;
    private int lastAvailableItemHash;

    private Map<Holder<Enchantment>, Integer> resolvedCache = Map.of();
    private final Set<Holder<Enchantment>> conflictCache = new HashSet<>();
    private boolean markedDirtyCache;
    private boolean computeCacheDirty = true;
    private int lastCacheSelectionsMod;
    private int lastCacheInputHash;

    private EditBox searchBox;
    private Button enchantButton;
    private Button repairButton;

    public EnchantingAltarScreen(EnchantingAltarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
        this.imageWidth = IMAGE_W;
        this.imageHeight = IMAGE_H;
        this.inventoryLabelX = 30;
        this.inventoryLabelY = IMAGE_H - 94;
    }

    @Override
    protected void init() {
        super.init();
        String previousSearch = searchBox != null ? searchBox.getValue() : "";
        listDirty = true;
        tooltipDirty = true;
        computeCacheDirty = true;
        reloadAvailableIfDirty();
        recomputeCacheIfNeeded();

        searchBox = new EditBox(font, leftPos + 67, topPos + 6, 116, 9,
                Component.translatable("attributearch.gui.enchant_search"));
        searchBox.setMaxLength(50);
        searchBox.setBordered(false);
        searchBox.setTextColor(-1);
        searchBox.setResponder(text -> {
            scrollOffset = 0;
            refreshFiltered();
        });
        searchBox.setValue(previousSearch);
        this.addWidget(searchBox);

        enchantButton = Button.builder(Component.literal("0"), b -> confirm())
                .bounds(leftPos + 7, topPos + 44, 18, 18)
                .build();
        enchantButton.active = false;
        addRenderableWidget(enchantButton);

        repairButton = Button.builder(
                        Component.translatable("attributearch.gui.enchant_repair_icon"),
                        b -> clickButton(EnchantingAltarMenu.BUTTON_REPAIR))
                .bounds(leftPos + 7, topPos + 66, 18, 18)
                .build();
        addRenderableWidget(repairButton);

        refreshButtons();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String s = this.searchBox != null ? this.searchBox.getValue() : "";
        super.resize(minecraft, width, height);
        if (this.searchBox != null) {
            this.searchBox.setValue(s);
            this.refreshFiltered();
        }
    }

    private void clickButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void confirm() {

        menu.sendConfirm(clientSelections);
    }

    private java.util.Map<ResourceLocation, Integer> getClientSelections() {
        return clientSelections;
    }

    private void setClientSelection(ResourceLocation id, int level) {

        clientSelections.put(id, Math.max(0, level));
    }

    private void tickClientInputSync() {
        int hash = inputHash();
        if (hash != lastInputFingerprint) {
            lastInputFingerprint = hash;
            clientSelections.clear();
        }
    }

    private void markSelectionsChanged() {
        listDirty = true;
        tooltipDirty = true;
        computeCacheDirty = true;
    }

    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private int inputHash() {
        ItemStack stack = menu.getInputItem();
        if (stack.isEmpty()) {
            return 0;
        }
        return 31 * stack.getItem().hashCode()
                + EnchantingAltarHelper.getAppliedEnchantments(stack).hashCode();
    }

    private int selectionsMod() {
        Map<ResourceLocation, Integer> selections = getClientSelections();
        return selections.size() * 31 + selections.hashCode();
    }

    private void reloadAvailableIfDirty() {
        ItemStack stack = menu.getInputItem();
        int hash = inputHash();
        if (!listDirty && hash == lastAvailableItemHash) {
            return;
        }
        lastAvailableItemHash = hash;
        available = EnchantingAltarHelper.listAvailable(player.level(), stack);
        listDirty = false;
        refreshFiltered();
    }

    private void recomputeCacheIfNeeded() {
        int selMod = selectionsMod();
        int hash = inputHash();
        if (!computeCacheDirty && selMod == lastCacheSelectionsMod && hash == lastCacheInputHash) {
            return;
        }
        ItemStack stack = menu.getInputItem();
        resolvedCache = EnchantingAltarHelper.resolveSelections(player.level(), stack, getClientSelections());
        conflictCache.clear();
        for (Holder<Enchantment> holder : available) {

            if (!EnchantingAltarHelper.findConflicts(player.level(), stack, holder, resolvedCache).isEmpty()) {
                conflictCache.add(holder);
            }
        }
        markedDirtyCache = EnchantingAltarHelper.isSelectionsDirty(
                player.level(), stack, getClientSelections());
        lastCacheSelectionsMod = selMod;
        lastCacheInputHash = hash;
        computeCacheDirty = false;
    }

    private int levelOf(Holder<Enchantment> holder, ItemEnchantments applied) {
        return resolvedCache.getOrDefault(holder, applied.getLevel(holder));
    }

    private boolean isConflicted(Holder<Enchantment> holder) {
        return conflictCache.contains(holder);
    }

    private void refreshFiltered() {
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        if (query.isEmpty()) {
            filtered = available;
        } else {
            List<Holder<Enchantment>> result = new ArrayList<>();
            for (Holder<Enchantment> holder : available) {
                String name = EnchantingAltarHelper.displayName(holder);
                ResourceLocation id = EnchantingAltarHelper.idOf(holder);
                String idStr = id == null ? "" : id.toString();

                if (EnchantPinyin.matches(holder, name, idStr, query)) {
                    result.add(holder);
                }
            }
            filtered = result;
        }
        int max = maxScroll();
        if (scrollOffset > max) {
            scrollOffset = max;
        }
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - MAX_VISIBLE);
    }

    private void refreshButtons() {
        recomputeCacheIfNeeded();
        ItemStack stack = menu.getInputItem();
        boolean hasItem = !stack.isEmpty();

        int cost = EnchantingAltarHelper.effectiveCost(player.level(), stack, getClientSelections());
        boolean canAfford = EnchantingAltarHelper.canAfford(player, cost);
        boolean markedDirty = markedDirtyCache;

        if (enchantButton != null) {

            boolean mayApply = hasItem && canAfford && markedDirty;
            enchantButton.setMessage(mayApply
                    ? Component.literal("✓")
                    : Component.literal(String.valueOf(cost)));
            boolean hasSelection = !getClientSelections().isEmpty();
            enchantButton.active = mayApply;
            if (tooltipDirty) {
                enchantButton.setTooltip(Tooltip.create(
                        buildEnchantTooltip(stack, cost, hasItem, hasSelection, markedDirty, canAfford, mayApply)));
            }
        }
        if (repairButton != null) {
            boolean canRepair = hasItem && stack.isDamageableItem() && stack.getDamageValue() > 0;
            repairButton.active = canRepair;
            if (tooltipDirty) {
                if (canRepair) {
                    repairButton.setTooltip(Tooltip.create(buildRepairTooltip(stack)));
                } else {
                    repairButton.setTooltip(null);
                }
            }
        }
    }

    private static final Component NEWLINE = Component.literal("\n");

    private Component buildEnchantTooltip(ItemStack stack, int cost, boolean hasItem, boolean hasSelection,
                                          boolean markedDirty, boolean canAfford, boolean mayApply) {
        MutableComponent root = Component.empty();
        if (mayApply && !stack.isEmpty()) {
            root.append(stack.getHoverName().copy().withStyle(stack.getRarity().color()));
            root.append(NEWLINE);
            appendEnchantDiffLines(root, stack);
            root.append(NEWLINE);
            root.append(cost == 0
                    ? Component.translatable("attributearch.gui.enchant_free").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("attributearch.gui.enchant_cost", cost).withStyle(ChatFormatting.GRAY));
            return root;
        }
        if (!hasItem) {
            root.append(Component.translatable("attributearch.gui.enchant_need_item").withStyle(ChatFormatting.RED));
        } else if (!hasSelection) {
            root.append(Component.translatable("attributearch.gui.enchant_select_first").withStyle(ChatFormatting.RED));
        } else if (!markedDirty) {
            root.append(Component.translatable("attributearch.gui.enchant_no_change").withStyle(ChatFormatting.RED));
        } else if (!canAfford) {
            root.append(Component.translatable("attributearch.gui.enchant_cost", cost).withStyle(ChatFormatting.RED));
        } else {
            root.append(Component.translatable("attributearch.gui.enchant_need_item").withStyle(ChatFormatting.RED));
        }
        return root;
    }

    private void appendEnchantDiffLines(MutableComponent root, ItemStack stack) {
        ItemEnchantments original = EnchantingAltarHelper.getAppliedEnchantments(stack);
        recomputeCacheIfNeeded();
        Map<Holder<Enchantment>, Integer> resolved = resolvedCache;
        ItemEnchantments target = EnchantingAltarHelper.mergeSelections(stack, resolved);

        List<Component> newLines = new ArrayList<>();
        List<Component> changedLines = new ArrayList<>();
        List<Component> removedLines = new ArrayList<>();
        List<Component> unchangedLines = new ArrayList<>();

        for (Object2IntEntry entry : collectEnchants(target)) {
            int oldLevel = original.getLevel(entry.holder());
            int newLevel = entry.level();
            if (newLevel > 0 && oldLevel == 0) {
                newLines.add(displayWithLevel(entry.holder(), newLevel).copy().withStyle(ChatFormatting.GREEN));
            } else if (newLevel > 0 && newLevel != oldLevel) {
                MutableComponent change = Component.translatable(
                        "attributearch.gui.tooltip.change",
                        Component.translatable("enchantment.level." + oldLevel),
                        Component.translatable("enchantment.level." + newLevel));
                changedLines.add(displayNamePlain(entry.holder()).append(CommonComponents.SPACE)
                        .append(change).withStyle(ChatFormatting.YELLOW));
            } else if (newLevel > 0) {
                unchangedLines.add(displayWithLevel(entry.holder(), newLevel).copy().withStyle(ChatFormatting.GRAY));
            }
        }
        for (Object2IntEntry entry : collectEnchants(original)) {
            if (target.getLevel(entry.holder()) <= 0) {
                removedLines.add(displayWithLevel(entry.holder(), entry.level()).copy().withStyle(ChatFormatting.RED));
            }
        }

        boolean first = true;
        for (List<Component> group : List.of(newLines, changedLines, unchangedLines, removedLines)) {
            for (Component line : group) {
                if (!first) {
                    root.append(NEWLINE);
                }
                root.append(line);
                first = false;
            }
        }
        if (first) {
            root.append(Component.translatable("attributearch.gui.enchant_no_change")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private record Object2IntEntry(Holder<Enchantment> holder, int level) {
    }

    private List<Object2IntEntry> collectEnchants(ItemEnchantments enchantments) {
        List<Object2IntEntry> list = new ArrayList<>();
        for (var entry : enchantments.entrySet()) {
            if (entry.getIntValue() > 0) {
                list.add(new Object2IntEntry(entry.getKey(), entry.getIntValue()));
            }
        }
        list.sort(Comparator.comparing(e -> displayNamePlain(e.holder()).getString()));
        return list;
    }

    private Component buildRepairTooltip(ItemStack stack) {
        MutableComponent root = Component.empty();
        root.append(stack.getHoverName().copy().withStyle(stack.getRarity().color()));
        root.append(NEWLINE);
        root.append(Component.translatable(
                "attributearch.gui.tooltip.durability",
                Component.literal(String.valueOf(stack.getMaxDamage() - stack.getDamageValue())),
                Component.literal(String.valueOf(stack.getMaxDamage()))
        ).withStyle(ChatFormatting.YELLOW));
        root.append(NEWLINE);
        root.append(Component.translatable("attributearch.gui.enchant_free").withStyle(ChatFormatting.GRAY));
        return root;
    }

    @Override
    public void containerTick() {
        super.containerTick();

        tickClientInputSync();
        int selMod = selectionsMod();
        int inputHash = inputHash();
        if (selMod != lastSelectionsModCount || inputHash != lastAvailableItemHash) {
            if (selMod != lastSelectionsModCount) {
                listDirty = true;
            }
            tooltipDirty = true;
            computeCacheDirty = true;
            lastSelectionsModCount = selMod;
        }
        reloadAvailableIfDirty();
        refreshButtons();
        tooltipDirty = false;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(GUI_TEXTURE, leftPos, topPos, 0, 0, IMAGE_W, IMAGE_H);
        graphics.blit(GUI_TEXTURE, leftPos + 8 - 1, topPos + 23 - 1, 196, 185, 18, 18);
        if (searchBox != null) {
            searchBox.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        recomputeCacheIfNeeded();
        renderEnchantList(guiGraphics, mouseX, mouseY);
        renderRowTooltip(guiGraphics, mouseX, mouseY);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderRowTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int listX = leftPos + LIST_X;
        int listY = topPos + LIST_Y;
        if (mouseX < listX || mouseX >= listX + LIST_W || mouseY < listY || mouseY >= listY + VISIBLE_H) {
            return;
        }
        int row = Mth.clamp((int) ((mouseY - listY) / ROW_H), 0, MAX_VISIBLE - 1);
        int index = scrollOffset + row;
        if (index >= filtered.size()) {
            return;
        }
        boolean onLeft = mouseX < listX + 18;
        boolean onRight = mouseX >= listX + LIST_W - 18;
        if (onLeft || onRight) {
            return;
        }
        Holder<Enchantment> holder = filtered.get(index);
        ItemStack stack = menu.getInputItem();
        recomputeCacheIfNeeded();
        int level = levelOf(holder, EnchantingAltarHelper.getAppliedEnchantments(stack));
        boolean present = level > 0;
        boolean conflict = present && isConflicted(holder);

        List<Component> lines = new ArrayList<>();
        if (conflict) {
            List<Holder<Enchantment>> conflicts =
                    EnchantingAltarHelper.findConflicts(player.level(), stack, holder, resolvedCache);
            lines.add(Component.translatable("attributearch.gui.tooltip.incompatible",
                    buildIncompatibleList(conflicts)).withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(displayNamePlain(holder).append(CommonComponents.SPACE)
                    .append(buildLevelRangeComponent(holder)).withStyle(ChatFormatting.WHITE));
            String descKey = descriptionKey(holder);
            if (descKey != null) {
                lines.add(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));
            }
            if (present) {
                lines.add(Component.translatable("attributearch.gui.tooltip.current",
                        Component.translatable("enchantment.level." + level))
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        List<FormattedCharSequence> tooltipLines = new ArrayList<>(lines.size());
        for (Component line : lines) {
            tooltipLines.addAll(font.split(line, 200));
        }
        this.setTooltipForNextRenderPass(tooltipLines);
    }

    private Component buildLevelRangeComponent(Holder<Enchantment> holder) {
        int min = holder.value().getMinLevel();
        int max = EnchantingAltarHelper.getMaxLevel(holder);
        MutableComponent levels = Component.translatable("enchantment.level." + min);
        if (min != max) {
            levels.append("-").append(Component.translatable("enchantment.level." + max));
        }
        return Component.literal("(").append(levels).append(")").withStyle(ChatFormatting.GRAY);
    }

    private Component buildIncompatibleList(List<Holder<Enchantment>> conflicts) {
        MutableComponent joined = Component.empty();
        boolean first = true;
        for (Holder<Enchantment> other : conflicts) {
            if (!first) {
                joined.append(", ");
            }
            joined.append(displayNamePlain(other));
            first = false;
        }
        return joined.withStyle(ChatFormatting.GRAY);
    }

    @Nullable
    private static String descriptionKey(Holder<Enchantment> holder) {
        String translationKey = holder.unwrapKey().map((ResourceKey<Enchantment> resourceKey) ->
                Util.makeDescriptionId(resourceKey.registry().getPath(), resourceKey.location())
        ).orElse(null);
        if (translationKey == null) {
            return null;
        }
        if (Language.getInstance().has(translationKey + ".desc")) {
            return translationKey + ".desc";
        }
        if (Language.getInstance().has(translationKey + ".description")) {
            return translationKey + ".description";
        }
        return null;
    }

    private static MutableComponent displayNamePlain(Holder<Enchantment> holder) {
        return holder.value().description().copy().setStyle(Style.EMPTY);
    }

    private static Component displayWithLevel(Holder<Enchantment> holder, int level) {
        MutableComponent component = displayNamePlain(holder);
        if (level != 1 || EnchantingAltarHelper.getMaxLevel(holder) != 1) {
            return component.append(CommonComponents.SPACE)
                    .append(Component.translatable("enchantment.level." + level));
        }
        return component;
    }

    private int rowFontColor(Holder<Enchantment> holder, int level, boolean conflict) {
        if (conflict) {

            return 0xA89C88;
        }
        if (level > 0) {

            return 0x2A2410;
        }
        if (EnchantingAltarHelper.isTreasure(holder)) {
            return ChatFormatting.GOLD.getColor();
        }
        if (EnchantingAltarHelper.isCurse(holder)) {
            return ChatFormatting.LIGHT_PURPLE.getColor();
        }
        return 0xFFFFFF;
    }

    private void renderEnchantList(GuiGraphics g, int mouseX, int mouseY) {
        ItemStack stack = menu.getInputItem();
        ItemEnchantments applied = EnchantingAltarHelper.getAppliedEnchantments(stack);
        recomputeCacheIfNeeded();
        int listX = leftPos + LIST_X;
        int listY = topPos + LIST_Y;

        if (filtered.isEmpty()) {
            Component msg;
            if (stack.isEmpty()) {
                msg = Component.translatable("attributearch.gui.enchant_item");
            } else if (available.isEmpty()) {
                msg = Component.translatable("attributearch.gui.enchant_empty_list");
            } else {
                msg = Component.translatable("attributearch.gui.enchant_no_match");
            }
            int msgY = listY + (VISIBLE_H - 8) / 2;
            g.enableScissor(listX, listY, listX + LIST_W, listY + VISIBLE_H);
            g.drawString(font, msg, listX + (LIST_W - font.width(msg)) / 2, msgY, 0x808080, false);
            g.disableScissor();
            padListBottom(g, listX, listY);
            return;
        }

        for (int i = 0; i < MAX_VISIBLE; i++) {
            int index = scrollOffset + i;
            if (index >= filtered.size()) {
                break;
            }
            int top = listY + i * ROW_H;
            Holder<Enchantment> holder = filtered.get(index);
            int level = levelOf(holder, applied);
            boolean present = level > 0;

            boolean conflict = isConflicted(holder);

            int yImage = conflict ? 0 : (present ? 2 : 1);

            g.blit(GUI_TEXTURE, listX, top, LIST_W, ROW_H,
                    0, 185 + yImage * SRC_ROW_H, LIST_W, SRC_ROW_H, 256, 256);
            if (conflict) {
                g.fill(listX, top, listX + LIST_W, top + ROW_H, 0x60000000);
            }

            Component label = present
                    ? Enchantment.getFullname(holder, level)
                    : displayNamePlain(holder);
            int fontColor = rowFontColor(holder, level, conflict);
            drawRowLabel(g, label, listX + 18 + 2, top, listX + LIST_W - 18 - 2, top + ROW_H, fontColor);

            int removeU = 220;
            int addU = 238;
            boolean hoverRemove = mouseX >= listX && mouseX < listX + 18 && mouseY >= top && mouseY < top + ROW_H;
            boolean hoverAdd = mouseX >= listX + LIST_W - 18 && mouseX < listX + LIST_W
                    && mouseY >= top && mouseY < top + ROW_H;
            boolean hoverMiddle = !hoverRemove && !hoverAdd
                    && mouseX >= listX + 18 && mouseX < listX + LIST_W - 18
                    && mouseY >= top && mouseY < top + ROW_H;
            if (hoverMiddle && !conflict) {
                g.fill(listX + 18, top, listX + LIST_W - 18, top + ROW_H, 0x40FFFFFF);
            }

            if (present) {
                int img = hoverRemove ? 2 : 1;
                g.blit(GUI_TEXTURE, listX, top, 18, ROW_H,
                        removeU, img * SRC_ROW_H, 18, SRC_ROW_H, 256, 256);
            }
            int maxLevel = EnchantingAltarHelper.getMaxLevel(holder);
            if (level < maxLevel && !conflict) {
                int img = hoverAdd ? 2 : 1;
                g.blit(GUI_TEXTURE, listX + LIST_W - 18, top, 18, ROW_H,
                        addU, img * SRC_ROW_H, 18, SRC_ROW_H, 256, 256);
            }
        }

        if (filtered.size() > MAX_VISIBLE) {
            int trackH = VISIBLE_H;
            int thumbH = Math.max(SCROLLBAR_H, trackH * MAX_VISIBLE / filtered.size());
            int max = maxScroll();
            int thumbY = listY + (int) ((trackH - thumbH) * ((max == 0 ? 0 : (double) scrollOffset) / max));
            int scrollbarX = listX + LIST_W + SCROLLBAR_OFFSET;
            ResourceLocation scroller = max > 0
                    ? ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller")
                    : ResourceLocation.withDefaultNamespace("container/creative_inventory/scroller_disabled");
            g.blitSprite(scroller, scrollbarX, thumbY, SCROLLBAR_W, thumbH);
        }
        padListBottom(g, listX, listY);
    }

    private void padListBottom(GuiGraphics g, int listX, int listY) {
        if (VISIBLE_H >= LIST_WINDOW_H) {
            return;
        }
        int padTop = listY + VISIBLE_H;
        int padBot = listY + LIST_WINDOW_H + 1;
        g.fill(listX - 1, padTop, listX + LIST_W + 1, padBot, 0xFFC6C6C6);
        g.fill(listX - 1, padTop, listX, padTop + 1, 0xFF373737);
        g.fill(listX, padTop, listX + LIST_W, padTop + 1, 0xFFFFFFFF);
        g.fill(listX + LIST_W, padTop, listX + LIST_W + 1, padTop + 1, 0xFFFFFFFF);
    }

    private void drawRowLabel(GuiGraphics g, Component text, int left, int top, int right, int bottom, int color) {
        int w = font.width(text);
        int x = (left + right) / 2 - w / 2;
        int y = top + (bottom - top - 8) / 2;
        g.enableScissor(left, top, right, bottom);
        g.drawString(font, text, x, y, color, false);
        g.disableScissor();
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        int listX = leftPos + LIST_X;
        int listY = topPos + LIST_Y;
        int scrollbarX = listX + LIST_W + SCROLLBAR_OFFSET;
        return mouseX >= scrollbarX && mouseX < scrollbarX + SCROLLBAR_W
                && mouseY >= listY && mouseY < listY + VISIBLE_H;
    }

    private void setScrollFromMouse(double mouseY) {
        int listY = topPos + LIST_Y;
        int trackH = VISIBLE_H;
        int thumbH = Math.max(SCROLLBAR_H, trackH * MAX_VISIBLE / Math.max(1, filtered.size()));
        double scrollOffs = (mouseY - listY - thumbH / 2.0) / Math.max(1, (trackH - thumbH));
        int max = maxScroll();
        scrollOffset = Mth.clamp((int) (scrollOffs * max), 0, max);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar && button == 0) {
            setScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void applyLevelChange(Holder<Enchantment> holder, int next) {
        ResourceLocation id = EnchantingAltarHelper.idOf(holder);
        if (id == null) {
            return;
        }
        setClientSelection(id, next);
        markSelectionsChanged();
        refreshButtons();
        playClickSound();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && filtered.size() > MAX_VISIBLE && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            setScrollFromMouse(mouseY);
            return true;
        }
        if (button == 0 || button == 1) {
            int listX = leftPos + LIST_X;
            int listY = topPos + LIST_Y;
            if (mouseX >= listX && mouseX < listX + LIST_W && mouseY >= listY && mouseY < listY + VISIBLE_H) {
                int row = Mth.clamp((int) ((mouseY - listY) / ROW_H), 0, MAX_VISIBLE - 1);
                int index = scrollOffset + row;
                if (index < filtered.size()) {
                    Holder<Enchantment> holder = filtered.get(index);
                    int max = EnchantingAltarHelper.getMaxLevel(holder);
                    ItemStack stack = menu.getInputItem();
                    recomputeCacheIfNeeded();
                    int current = levelOf(holder, EnchantingAltarHelper.getAppliedEnchantments(stack));
                    boolean onLeft = mouseX < listX + 18;
                    boolean onRight = mouseX >= listX + LIST_W - 18;

                    boolean conflict = isConflicted(holder);
                    if (conflict && current <= 0) {
                        return true;
                    }

                    if (button == 0) {
                        if (onLeft && current > 0) {

                            int next = Screen.hasShiftDown() ? 0 : current - 1;
                            applyLevelChange(holder, next);
                            return true;
                        }
                        if (onRight && current < max && !conflict) {

                            int next = Screen.hasShiftDown() ? max : current + 1;
                            applyLevelChange(holder, next);
                            return true;
                        }
                        if (!onLeft && !onRight && !conflict) {
                            if (Screen.hasShiftDown()) {

                                if (current != max) {
                                    applyLevelChange(holder, max);
                                    return true;
                                }
                            } else if (current > 0) {
                                applyLevelChange(holder, 0);
                                return true;
                            } else {
                                applyLevelChange(holder, max);
                                return true;
                            }
                        }
                    } else if (button == 1 && current > 0) {

                        int next = Screen.hasShiftDown() ? 0 : current - 1;
                        applyLevelChange(holder, next);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listX = leftPos + LIST_X;
        int listY = topPos + LIST_Y;
        if (mouseX >= listX - 8 && mouseX <= listX + LIST_W + 16
                && mouseY >= listY && mouseY <= listY + VISIBLE_H) {
            int delta = scrollY > 0 ? -SCROLL_LINES : SCROLL_LINES;
            int next = Mth.clamp(scrollOffset + delta, 0, maxScroll());
            if (next != scrollOffset) {
                scrollOffset = next;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.ignoreTextInput) {
            return false;
        }
        if (searchBox != null && searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        this.ignoreTextInput = false;
        if (searchBox != null && searchBox.isFocused()) {
            boolean flag = this.hoveredSlot != null && this.hoveredSlot.hasItem();
            boolean flag1 = InputConstants.getKey(keyCode, scanCode).getNumericKeyValue().isPresent();
            if (flag && flag1 && this.checkHotbarKeyPressed(keyCode, scanCode)) {
                this.ignoreTextInput = true;
                return true;
            }
            if (keyCode == 256) {
                searchBox.setFocused(false);
                return true;
            }
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return keyCode != InputConstants.KEY_ESCAPE || super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (minecraft != null && minecraft.options.keyChat.matches(keyCode, scanCode)) {
            this.ignoreTextInput = true;
            searchBox.setFocused(true);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        this.ignoreTextInput = false;
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
