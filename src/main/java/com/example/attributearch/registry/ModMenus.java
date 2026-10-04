package com.example.attributearch.registry;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.menu.AttributeAltarMenu;
import com.example.attributearch.menu.EnchantingAltarMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    private ModMenus() {
    }

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, AttributeArch.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AttributeAltarMenu>> ATTRIBUTE_ALTAR =
            MENUS.register("attribute_altar", () ->
                    IMenuTypeExtension.create(AttributeAltarMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<EnchantingAltarMenu>> ENCHANTING_ALTAR =
            MENUS.register("enchanting_altar", () ->
                    IMenuTypeExtension.create(EnchantingAltarMenu::fromNetwork));
}
