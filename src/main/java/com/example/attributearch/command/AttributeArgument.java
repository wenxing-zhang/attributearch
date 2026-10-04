package com.example.attributearch.command;

import java.util.concurrent.CompletableFuture;

import com.example.attributearch.attribute.EnhanceableAttributes;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;

public class AttributeArgument implements ArgumentType<ResourceLocation> {
    private static final SimpleCommandExceptionType INVALID =
            new SimpleCommandExceptionType(Component.translatable("argument.attributearch.attribute.invalid"));

    @Override
    public ResourceLocation parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }
        String token = reader.getString().substring(start, reader.getCursor());
        if (token.isEmpty()) {
            throw INVALID.createWithContext(reader);
        }

        ResourceLocation asId = ResourceLocation.tryParse(token);
        if (asId != null && BuiltInRegistries.ATTRIBUTE.containsKey(asId)) {
            return asId;
        }

        for (Holder.Reference<Attribute> holder : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            String key = EnhanceableAttributes.holderKey(holder);
            if (key.equalsIgnoreCase(token) || EnhanceableAttributes.displayName(holder).equalsIgnoreCase(token)) {
                return holder.key().location();
            }
        }
        throw INVALID.createWithContext(reader);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {

        String remaining = builder.getRemainingLowerCase();
        for (Holder.Reference<Attribute> holder : BuiltInRegistries.ATTRIBUTE.holders().toList()) {
            String id = holder.key().location().toString();
            String display = EnhanceableAttributes.displayName(holder);
            if (remaining.isEmpty()
                    || display.toLowerCase(java.util.Locale.ROOT).contains(remaining)
                    || id.toLowerCase(java.util.Locale.ROOT).contains(remaining)) {
                builder.suggest(id, Component.translatable(holder.value().getDescriptionId()));
            }
        }
        return builder.buildFuture();
    }

    public static AttributeArgument attribute() {
        return new AttributeArgument();
    }
}
