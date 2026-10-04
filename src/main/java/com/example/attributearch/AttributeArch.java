package com.example.attributearch;

import com.example.attributearch.attachment.ModAttachments;
import com.example.attributearch.compat.curios.CuriosCompat;
import com.example.attributearch.config.ModConfig;
import com.example.attributearch.effect.ModEffects;
import com.example.attributearch.network.ModNetwork;
import com.example.attributearch.registry.ModArgumentTypes;
import com.example.attributearch.registry.ModBlockEntities;
import com.example.attributearch.registry.ModBlocks;
import com.example.attributearch.registry.ModCreativeTabs;
import com.example.attributearch.registry.ModEntityTypes;
import com.example.attributearch.registry.ModItems;
import com.example.attributearch.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig.Type;
import org.slf4j.Logger;

@Mod(AttributeArch.MODID)
public class AttributeArch {
    public static final String MODID = "attributearch";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AttributeArch(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModArgumentTypes.ARGUMENT_TYPES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        com.example.attributearch.attribute.ModAttributes.ATTRIBUTES.register(modEventBus);

        modEventBus.addListener(ModNetwork::onRegisterPayloads);

        modEventBus.addListener(AttributeArch::commonSetup);
        modEventBus.addListener(AttributeArch::onLoadComplete);

        modContainer.registerConfig(Type.COMMON, ModConfig.SPEC);
    }

    private static void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        event.enqueueWork(CuriosCompat::init);
        event.enqueueWork(com.example.attributearch.compat.tacz.TaczGunCompat::init);
        event.enqueueWork(com.example.attributearch.compat.spell.SpellAmplificationCompat::init);
    }

    private static void onLoadComplete(net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent event) {
        event.enqueueWork(com.example.attributearch.compat.attributefix.AttributeFixBypass::apply);
    }
}
