package com.example.attributearch.menu;

import com.example.attributearch.attachment.ModAttachments;
import com.example.attributearch.attribute.AttributeHelper;
import com.example.attributearch.blockentity.AttributeAltarBlockEntity;
import com.example.attributearch.network.SyncPlayerLevelsPayload;
import com.example.attributearch.registry.ModBlocks;
import com.example.attributearch.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class AttributeAltarMenu extends AbstractContainerMenu {
    public static final int BUTTON_ENHANCE = 0;
    public static final int BUTTON_DOWNGRADE = 1;

    private final Player player;
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    @Nullable
    private ResourceLocation selected;

    public static AttributeAltarMenu fromNetwork(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ResourceLocation selected = buf.readBoolean() ? buf.readResourceLocation() : null;
        return new AttributeAltarMenu(containerId, inventory, pos, selected);
    }

    public AttributeAltarMenu(int containerId, Inventory inventory, BlockPos pos, @Nullable ResourceLocation selected) {
        super(ModMenus.ATTRIBUTE_ALTAR.get(), containerId);
        this.player = inventory.player;
        this.access = ContainerLevelAccess.create(player.level(), pos);
        this.pos = pos;
        this.selected = selected;

        if (!player.level().isClientSide) {
            syncLevelsToClient();
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public ResourceLocation getSelected() {
        return selected;
    }

    public void setSelected(@Nullable ResourceLocation selected) {
        this.selected = selected;
        if (!player.level().isClientSide) {
            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof AttributeAltarBlockEntity altar) {
                altar.setSelected(selected);
            }
        }
    }

    public void syncLevelsToClient() {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PacketDistributor.sendToPlayer(
                serverPlayer,
                new SyncPlayerLevelsPayload(player.getData(ModAttachments.ENHANCE_LEVELS)));
    }

    @Override
    public boolean clickMenuButton(Player clicker, int buttonId) {

        if (clicker.level().isClientSide) {
            return false;
        }
        if (selected == null) {
            return false;
        }
        boolean ok = switch (buttonId) {
            case BUTTON_ENHANCE -> AttributeHelper.tryEnhance(clicker, selected);
            case BUTTON_DOWNGRADE -> AttributeHelper.tryDowngrade(clicker, selected);
            default -> false;
        };
        if (ok) {
            syncLevelsToClient();
        }
        return ok;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ATTRIBUTE_ALTAR.get());
    }
}
