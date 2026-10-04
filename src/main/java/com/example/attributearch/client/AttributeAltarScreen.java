package com.example.attributearch.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.attribute.AttributeHelper;
import com.example.attributearch.attribute.AttributePinyin;
import com.example.attributearch.attribute.EnhanceableAttributes;
import com.example.attributearch.config.ModConfig;
import com.example.attributearch.menu.AttributeAltarMenu;
import com.example.attributearch.network.SelectAttributePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class AttributeAltarScreen extends AbstractContainerScreen<AttributeAltarMenu> {
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 140;
    private static final int MAX_VISIBLE = 6;
    private static final int ROW_H = 14;

    private static final int COL_BG = 0xFFC6C6C6;
    private static final int COL_BORDER = 0xFF555555;
    private static final int COL_INSET = 0xFF3C3C3C;
    private static final int COL_LIST = 0xFF2A2A3A;
    private static final int COL_LIST_HOVER = 0xFF4A4A6A;
    private static final int COL_LIST_SEL = 0xFF3A5A8A;

    private static final int SEARCH_X = 8;
    private static final int SEARCH_Y = 28;
    private static final int SEARCH_W = 148;
    private static final int SEARCH_H = 16;

    private static final int LIST_X = 8;
    private static final int LIST_Y = 48;
    private static final int LIST_W = 160;
    private static final int LIST_H = MAX_VISIBLE * ROW_H + 4;

    private static final int STATUS_Y = 48;
    private static final int STATUS_H = 40;

    private static final int BTN_Y = 92;
    private static final int XP_Y = 114;

    private final Player player;
    private List<Holder<Attribute>> attributes = List.of();
    private List<Holder<Attribute>> filtered = List.of();
    private int selectedIndex = -1;
    private boolean dropdownOpen;
    private int scrollOffset;

    private EditBox searchBox;
    private Button enhanceButton;
    private Button downgradeButton;

    public AttributeAltarScreen(AttributeAltarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.player = playerInventory.player;
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = imageHeight;
    }

    @Override
    protected void init() {
        super.init();
        reloadAttributes();

        searchBox = new EditBox(font, leftPos + SEARCH_X + 1, topPos + SEARCH_Y + 1, SEARCH_W - 2, SEARCH_H - 2,
                Component.translatable("attributearch.gui.search"));
        searchBox.setMaxLength(64);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setTextColorUneditable(0xA0A0A0);
        searchBox.setHint(Component.translatable("attributearch.gui.search"));
        searchBox.setResponder(text -> {
            scrollOffset = 0;
            refreshFiltered();
            if (!text.isEmpty()) {
                setDropdownOpen(true);
            }
        });
        addRenderableWidget(searchBox);

        enhanceButton = Button.builder(Component.translatable("attributearch.gui.enhance"),
                        b -> clickButton(AttributeAltarMenu.BUTTON_ENHANCE))
                .bounds(leftPos + 30, topPos + BTN_Y, 52, 18)
                .build();
        addRenderableWidget(enhanceButton);

        downgradeButton = Button.builder(Component.translatable("attributearch.gui.downgrade"),
                        b -> clickButton(AttributeAltarMenu.BUTTON_DOWNGRADE))
                .bounds(leftPos + 94, topPos + BTN_Y, 52, 18)
                .build();
        addRenderableWidget(downgradeButton);

        setDropdownOpen(dropdownOpen);
        refreshButtons();
        setInitialFocus(searchBox);
    }

    @Override
    public void resize(net.minecraft.client.Minecraft minecraft, int width, int height) {
        String s = this.searchBox != null ? this.searchBox.getValue() : "";
        boolean open = this.dropdownOpen;
        super.resize(minecraft, width, height);
        if (this.searchBox != null) {
            this.searchBox.setValue(s);
            this.dropdownOpen = open;
            this.refreshFiltered();
            this.setDropdownOpen(open);
        }
    }

    private void setDropdownOpen(boolean open) {
        dropdownOpen = open;
        if (enhanceButton != null) {
            enhanceButton.visible = !open;
        }
        if (downgradeButton != null) {
            downgradeButton.visible = !open;
        }
    }

    private void reloadAttributes() {
        attributes = EnhanceableAttributes.list(player);
        ResourceLocation selected = menu.getSelected();
        selectedIndex = -1;
        if (selected != null) {
            for (int i = 0; i < attributes.size(); i++) {
                if (selected.equals(BuiltInRegistries.ATTRIBUTE.getKey(attributes.get(i).value()))) {
                    selectedIndex = i;
                    break;
                }
            }
        }
        refreshFiltered();
    }

    private void refreshFiltered() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            filtered = attributes;
        } else {
            List<Holder<Attribute>> result = new ArrayList<>();
            for (Holder<Attribute> holder : attributes) {
                String name = EnhanceableAttributes.displayName(holder);
                ResourceLocation rid = holder.unwrapKey().map(k -> k.location()).orElse(null);

                if (AttributePinyin.matches(rid, name, query)) {
                    result.add(holder);
                }
            }
            filtered = result;
        }
        if (scrollOffset > maxScroll()) {
            scrollOffset = maxScroll();
        }
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - MAX_VISIBLE);
    }

    private void clickButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    private void selectFiltered(int filteredIndex) {
        if (filteredIndex < 0 || filteredIndex >= filtered.size()) {
            return;
        }
        Holder<Attribute> holder = filtered.get(filteredIndex);
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(holder.value());
        if (id == null) {
            return;
        }
        selectedIndex = -1;
        for (int i = 0; i < attributes.size(); i++) {
            if (attributes.get(i).equals(holder)) {
                selectedIndex = i;
                break;
            }
        }
        setDropdownOpen(false);
        PacketDistributor.sendToServer(new SelectAttributePayload(id));
        menu.setSelected(id);
        refreshButtons();
    }

    private Holder<Attribute> selectedHolder() {
        if (selectedIndex >= 0 && selectedIndex < attributes.size()) {
            return attributes.get(selectedIndex);
        }
        return null;
    }

    private ResourceLocation selectedId() {
        Holder<Attribute> holder = selectedHolder();
        return holder == null ? null : BuiltInRegistries.ATTRIBUTE.getKey(holder.value());
    }

    private void refreshButtons() {
        ResourceLocation id = selectedId();
        if (id == null) {
            if (enhanceButton != null) {
                enhanceButton.active = false;
            }
            if (downgradeButton != null) {
                downgradeButton.active = false;
            }
            return;
        }
        int level = ClientLevelCache.getLevel(id);
        int max = AttributeHelper.getMaxLevel(id);
        int cost = AttributeHelper.getCostForNextLevel(level);
        if (enhanceButton != null) {
            boolean canPay = player.experienceLevel >= cost || player.getAbilities().instabuild;
            enhanceButton.active = level < max && canPay;
        }
        if (downgradeButton != null) {
            downgradeButton.active = level > 0;
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        refreshButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, COL_BG);
        drawBorder(graphics, x, y, imageWidth, imageHeight);

        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + 17, 0xFF8B8B8B);

        graphics.fill(x + SEARCH_X, y + SEARCH_Y, x + SEARCH_X + SEARCH_W, y + SEARCH_Y + SEARCH_H, COL_INSET);
        drawBorder(graphics, x + SEARCH_X, y + SEARCH_Y, SEARCH_W, SEARCH_H, COL_BORDER);

        if (dropdownOpen) {
            graphics.fill(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + LIST_H, COL_LIST);
            drawBorder(graphics, x + LIST_X, y + LIST_Y, LIST_W, LIST_H, COL_BORDER);
        } else {
            graphics.fill(x + LIST_X, y + STATUS_Y, x + LIST_X + LIST_W, y + STATUS_Y + STATUS_H, COL_INSET);
            drawBorder(graphics, x + LIST_X, y + STATUS_Y, LIST_W, STATUS_H, COL_BORDER);
        }
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h) {
        drawBorder(g, x, y, w, h, 0xFF000000);
    }

    private static void drawBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 5, 0x404040, false);

        String selectedName = selectedHolder() != null
                ? EnhanceableAttributes.displayName(selectedHolder())
                : Component.translatable("attributearch.gui.none").getString();
        graphics.drawString(font, selectedName, 8, 19, 0xFFFFFF, false);

        if (!dropdownOpen) {
            ResourceLocation id = selectedId();
            int level = ClientLevelCache.getLevel(id);
            int max = AttributeHelper.getMaxLevel(id);
            int cost = AttributeHelper.getCostForNextLevel(level);
            boolean amplification = com.example.attributearch.attribute.ModAttributes.isAmplification(id);

            if (amplification) {
                boolean on = level >= com.example.attributearch.attribute.ModAttributes.MAX_AMPLIFICATION_LEVEL;
                graphics.drawString(font,
                        Component.translatable(on
                                        ? "attributearch.gui.amp_on"
                                        : "attributearch.gui.amp_off").getString(),
                        LIST_X + 4, STATUS_Y + 4, 0xFFFFFF, false);
            } else {
                graphics.drawString(font,
                        Component.translatable("attributearch.gui.level", level, max).getString(),
                        LIST_X + 4, STATUS_Y + 4, 0xFFFFFF, false);
            }

            if (level < max) {
                boolean enough = player.experienceLevel >= cost || player.getAbilities().instabuild;
                graphics.drawString(font,
                        Component.translatable("attributearch.gui.need_xp", cost).getString(),
                        LIST_X + 4, STATUS_Y + 16, enough ? 0x55FF55 : 0xFF5555, false);
                if (amplification) {
                    graphics.drawString(font,
                            Component.translatable("attributearch.gui.amp_next_on").getString(),
                            LIST_X + 4, STATUS_Y + 28, 0xCCCCCC, false);
                } else {
                    graphics.drawString(font,
                            Component.translatable("attributearch.gui.next",
                                    fmt(ModConfig.BASE_ADDITION_PER_LEVEL.get()),
                                    fmt(ModConfig.BASE_MULTIPLIER_PER_LEVEL.get()),
                                    fmt(ModConfig.BASE_INDEPENDENT_MULTIPLIER_PER_LEVEL.get())).getString(),
                            LIST_X + 4, STATUS_Y + 28, 0xCCCCCC, false);
                }
            } else {
                graphics.drawString(font,
                        Component.translatable("attributearch.gui.maxed").getString(),
                        LIST_X + 4, STATUS_Y + 16, 0xFFAA00, false);
            }

            graphics.drawString(font,
                    Component.translatable("attributearch.gui.xp", player.experienceLevel).getString(),
                    LIST_X + 4, XP_Y, 0x55FFFF, false);
        }
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1.0e-6) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.valueOf(v);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (dropdownOpen) {
            renderDropdown(graphics, mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderDropdown(GuiGraphics graphics, int mouseX, int mouseY) {
        int listX = leftPos + LIST_X;
        int listY = topPos + LIST_Y;

        graphics.fill(listX, listY, listX + LIST_W, listY + LIST_H, COL_LIST);
        drawBorder(graphics, listX, listY, LIST_W, LIST_H, COL_BORDER);

        int hover = hoveredRow(mouseX, mouseY);
        ResourceLocation selectedId = selectedId();

        for (int i = 0; i < MAX_VISIBLE; i++) {
            int index = scrollOffset + i;
            if (index >= filtered.size()) {
                break;
            }
            int rowY = listY + 2 + i * ROW_H;
            Holder<Attribute> holder = filtered.get(index);
            ResourceLocation rowId = BuiltInRegistries.ATTRIBUTE.getKey(holder.value());

            if (i == hover) {
                graphics.fill(listX + 1, rowY, listX + LIST_W - 1, rowY + ROW_H, COL_LIST_HOVER);
            } else if (rowId != null && rowId.equals(selectedId)) {
                graphics.fill(listX + 1, rowY, listX + LIST_W - 1, rowY + ROW_H, COL_LIST_SEL);
            }

            String label = EnhanceableAttributes.displayName(holder);
            graphics.enableScissor(listX + 3, rowY, listX + LIST_W - 3, rowY + ROW_H);
            graphics.drawString(font, label, listX + 5, rowY + 3, 0xFFFFFF, false);
            graphics.disableScissor();
        }

        if (filtered.isEmpty()) {
            graphics.drawString(font, "-", listX + 5, listY + 6, 0x808080, false);
        }

        if (filtered.size() > MAX_VISIBLE) {
            int trackY = listY + 2;
            int trackH = LIST_H - 4;
            int thumbH = Math.max(8, trackH * MAX_VISIBLE / filtered.size());
            int max = maxScroll();
            int thumbY = trackY + (trackH - thumbH) * (max == 0 ? 0 : scrollOffset) / Math.max(1, max);
            graphics.fill(listX + LIST_W - 3, thumbY, listX + LIST_W - 1, thumbY + thumbH, 0xFF909090);
        }
    }

    private int hoveredRow(double mouseX, double mouseY) {
        if (!dropdownOpen) {
            return -1;
        }
        int listX = leftPos + LIST_X;
        int listY = topPos + LIST_Y + 2;
        if (mouseX < listX || mouseX > listX + LIST_W || mouseY < listY || mouseY > listY + MAX_VISIBLE * ROW_H) {
            return -1;
        }
        return Mth.clamp((int) ((mouseY - listY) / ROW_H), 0, MAX_VISIBLE - 1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean inSearch = mouseX >= leftPos + SEARCH_X && mouseX <= leftPos + SEARCH_X + SEARCH_W
                && mouseY >= topPos + SEARCH_Y && mouseY <= topPos + SEARCH_Y + SEARCH_H;
        boolean inList = mouseX >= leftPos + LIST_X && mouseX <= leftPos + LIST_X + LIST_W
                && mouseY >= topPos + LIST_Y && mouseY <= topPos + LIST_Y + LIST_H;

        if (button == 0) {
            if (inSearch) {
                setDropdownOpen(true);
                setFocused(searchBox);
                return true;
            }
            if (dropdownOpen && inList) {
                int row = hoveredRow(mouseX, mouseY);
                if (row >= 0 && scrollOffset + row < filtered.size()) {
                    selectFiltered(scrollOffset + row);
                    return true;
                }
            }
            if (dropdownOpen && !inSearch && !inList) {
                setDropdownOpen(false);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (dropdownOpen) {
            boolean inList = mouseX >= leftPos + LIST_X && mouseX <= leftPos + LIST_X + LIST_W
                    && mouseY >= topPos + LIST_Y && mouseY <= topPos + LIST_Y + LIST_H;
            if (inList) {
                int delta = scrollY > 0 ? -1 : 1;
                int next = Mth.clamp(scrollOffset + delta, 0, maxScroll());
                if (next != scrollOffset) {
                    scrollOffset = next;
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && dropdownOpen) {
            setDropdownOpen(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchBox != null && searchBox.isFocused()) {
            setDropdownOpen(true);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
