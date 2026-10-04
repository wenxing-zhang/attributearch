package com.example.attributearch.blockentity;

import com.example.attributearch.menu.AttributeAltarMenu;
import com.example.attributearch.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AttributeAltarBlockEntity extends BlockEntity implements MenuProvider {
    private static final String TAG_SELECTED = "selected";

    @Nullable
    private ResourceLocation selected;

    public AttributeAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATTRIBUTE_ALTAR.get(), pos, state);
    }

    @Nullable
    public ResourceLocation getSelected() {
        return selected;
    }

    public void setSelected(@Nullable ResourceLocation selected) {
        this.selected = selected;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.attributearch.attribute_altar");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AttributeAltarMenu(containerId, inventory, worldPosition, selected);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_SELECTED, Tag.TAG_STRING)) {
            selected = ResourceLocation.tryParse(tag.getString(TAG_SELECTED));
        } else {
            selected = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (selected != null) {
            tag.putString(TAG_SELECTED, selected.toString());
        }
    }
}
