package com.example.attributearch.menu;

import com.example.attributearch.enchant.EnchantingAltarHelper;
import com.example.attributearch.network.ConfirmEnchantPayload;
import com.example.attributearch.registry.ModBlocks;
import com.example.attributearch.registry.ModMenus;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

public class EnchantingAltarMenu extends AbstractContainerMenu {
    public static final int BUTTON_ENCHANT = 0;
    public static final int BUTTON_REPAIR = 1;

    public static final int INPUT_SLOT = 0;

    private final Player player;
    private final ContainerLevelAccess access;
    private final BlockPos pos;

    private final Container inputContainer;

    public static EnchantingAltarMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new EnchantingAltarMenu(containerId, inventory, pos);
    }

    public EnchantingAltarMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, findInputContainer(inventory.player, pos), pos,
                ContainerLevelAccess.create(inventory.player.level(), pos));
    }

    public EnchantingAltarMenu(int containerId, Inventory inventory,
                               Container inputContainer, BlockPos pos, ContainerLevelAccess access) {
        super(ModMenus.ENCHANTING_ALTAR.get(), containerId);
        this.player = inventory.player;
        this.access = access;
        this.pos = pos;
        this.inputContainer = inputContainer;

        this.addSlot(new EnchantableSlot(inputContainer, 0, 8, 23) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (int k = 0; k < 4; k++) {
            EquipmentSlot equipmentSlot = armorSlots[k];
            ResourceLocation emptyIcon = switch (equipmentSlot) {
                case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
                case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
                case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
                default -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
            };
            this.addSlot(new EquipSlot(inventory, inventory.player, equipmentSlot, 39 - k,
                    8 + 188 * (k / 2), 103 + (k % 2) * 18, emptyIcon));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 30 + col * 18, 103 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 30 + col * 18, 161));
        }

        this.addSlot(new Slot(inventory, 40, 8, 161) {
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return Pair.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });
    }

    public BlockPos getPos() {
        return pos;
    }

    public ItemStack getInputItem() {
        return inputContainer.getItem(INPUT_SLOT);
    }

    public Container getInputContainer() {
        return inputContainer;
    }

    public void onInputChanged() {
        broadcastChanges();
    }

    public void sendConfirm(Map<ResourceLocation, Integer> selections) {
        if (player.level().isClientSide) {
            PacketDistributor.sendToServer(ConfirmEnchantPayload.fromMap(selections));
        }
    }

    @Override
    public boolean clickMenuButton(Player clicker, int buttonId) {
        if (clicker.level().isClientSide) {
            return false;
        }
        return switch (buttonId) {
            case BUTTON_REPAIR -> {
                ItemStack stack = getInputItem();
                boolean ok = EnchantingAltarHelper.tryRepair(stack);
                if (ok) {
                    inputContainer.setChanged();
                    onInputChanged();
                    clicker.level().playSound(null, pos,
                            net.minecraft.sounds.SoundEvents.ANVIL_USE,
                            net.minecraft.sounds.SoundSource.BLOCKS, 0.45F, 1.0F);
                }
                yield ok;
            }
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack current = slot.getItem();
            result = current.copy();

            if (index == INPUT_SLOT) {
                if (!this.moveItemStackTo(current, 5, 42, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (EnchantingAltarHelper.canInsert(current)) {
                if (!this.moveItemStackTo(current, INPUT_SLOT, INPUT_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 5 && index <= 31) {
                if (!this.moveItemStackTo(current, 32, 42, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 32) {
                if (!this.moveItemStackTo(current, 5, 32, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(current, 5, 42, true)) {
                return ItemStack.EMPTY;
            }
            if (current.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (current.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, current);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ENCHANTING_ALTAR.get());
    }

    private static Container findInputContainer(Player player, BlockPos pos) {
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return container;
        }
        return new SimpleContainer(1);
    }

    private static class EnchantableSlot extends Slot {
        public EnchantableSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return EnchantingAltarHelper.canInsert(stack);
        }
    }

    private static class EquipSlot extends Slot {
        private final Player owner;
        private final EquipmentSlot equipmentSlot;
        private final ResourceLocation emptyIcon;

        public EquipSlot(Container container, Player owner, EquipmentSlot equipmentSlot,
                         int slotIndex, int x, int y, ResourceLocation emptyIcon) {
            super(container, slotIndex, x, y);
            this.owner = owner;
            this.equipmentSlot = equipmentSlot;
            this.emptyIcon = emptyIcon;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(equipmentSlot, owner);
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            owner.onEquipItem(equipmentSlot, oldStack, newStack);
            super.setByPlayer(newStack, oldStack);
        }

        @Override
        public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, emptyIcon);
        }
    }
}
