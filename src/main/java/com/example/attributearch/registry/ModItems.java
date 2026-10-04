package com.example.attributearch.registry;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.item.WenxingBowItem;
import com.example.attributearch.item.WenxingTotemItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    private ModItems() {
    }

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AttributeArch.MODID);

    public static final DeferredItem<?> ATTRIBUTE_ALTAR =
            ITEMS.registerSimpleBlockItem("attribute_altar", ModBlocks.ATTRIBUTE_ALTAR);

    public static final DeferredItem<?> ENCHANTING_ALTAR =
            ITEMS.registerSimpleBlockItem("enchanting_altar", ModBlocks.ENCHANTING_ALTAR);

    public static final DeferredItem<WenxingTotemItem> WENXING_TOTEM =
            ITEMS.register("wenxing_totem", WenxingTotemItem::new);

    public static final DeferredItem<WenxingBowItem> WENXING_BOW =
            ITEMS.register("wenxing_bow",
                    () -> new WenxingBowItem(new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
                            .fireResistant()));
}
