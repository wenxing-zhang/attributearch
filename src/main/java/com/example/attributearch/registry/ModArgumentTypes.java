package com.example.attributearch.registry;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.command.AttributeArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModArgumentTypes {
    private ModArgumentTypes() {
    }

    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, AttributeArch.MODID);

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<AttributeArgument>> ATTRIBUTE =
            ARGUMENT_TYPES.register("attribute", () ->
                    ArgumentTypeInfos.registerByClass(
                            AttributeArgument.class,
                            SingletonArgumentInfo.contextFree(AttributeArgument::new)));
}
