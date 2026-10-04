package com.example.attributearch.command;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.attribute.AttributeHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = AttributeArch.MODID)
public final class ModCommands {
    private ModCommands() {
    }

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("attributearch")
                .requires(src -> Whitelist.isAllowed(src.getPlayer()) || src.hasPermission(3))
                .then(branch("add", Mode.ADD, IntegerArgumentType.integer(1)))
                .then(branch("set", Mode.SET, IntegerArgumentType.integer(0)))
                .then(branch("min", Mode.MIN, IntegerArgumentType.integer(1)))
                .then(Commands.literal("whitelist")
                        .requires(src -> src.hasPermission(3))
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    int count = Whitelist.reload();
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("commands.attributearch.whitelist.reloaded", count),
                                            true);
                                    return count;
                                }))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> branch(
            String name, Mode mode, IntegerArgumentType numType) {
        return Commands.literal(name)
                .then(Commands.argument("attribute", AttributeArgument.attribute())
                        .then(Commands.argument("value", numType)
                                .executes(ctx -> modify(ctx, mode, false))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> modify(ctx, mode, true)))));
    }

    private enum Mode {
        ADD, SET, MIN
    }

    private static int modify(CommandContext<CommandSourceStack> ctx, Mode mode, boolean hasPlayer) {
        try {
            ServerPlayer target = hasPlayer
                    ? EntityArgument.getPlayer(ctx, "player")
                    : ctx.getSource().getPlayerOrException();
            ResourceLocation attrId = ctx.getArgument("attribute", ResourceLocation.class);
            int value = IntegerArgumentType.getInteger(ctx, "value");

            int current = AttributeHelper.getLevel(target, attrId);

            int next = switch (mode) {
                case ADD -> {
                    long sum = (long) current + (long) value;
                    yield sum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
                }
                case SET -> value;
                case MIN -> Math.max(0, current - value);
            };

            AttributeHelper.setLevel(target, attrId, next);
            AttributeHelper.apply(target, attrId);
            if (target.containerMenu instanceof com.example.attributearch.menu.AttributeAltarMenu altarMenu) {
                altarMenu.syncLevelsToClient();
            }

            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.attributearch.modified",
                    attrId.toString(),
                    target.getGameProfile().getName(),
                    current,
                    AttributeHelper.getLevel(target, attrId)), true);
            return AttributeHelper.getLevel(target, attrId);
        } catch (Exception ex) {
            ctx.getSource().sendFailure(Component.literal(String.valueOf(ex.getMessage())));
            return 0;
        }
    }
}
