package com.chyzman.characteristic.command.argument;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static com.chyzman.characteristic.command.CharacterCommand.UNKNOWN_ERROR;

public class CharacterArgument extends NameArgument {
    public static final DynamicCommandExceptionType DUPLICATE = new DynamicCommandExceptionType((name) -> Component.translatable(
        "argument.characteristic.character.duplicate",
        name
    ));
    public static final DynamicCommandExceptionType NOT_FOUND = new DynamicCommandExceptionType((name) -> Component.translatable(
        "argument.characteristic.character.not_found",
        name
    ));

    private final Type type;

    private CharacterArgument(final Type type) {
        this.type = type;
    }

    public static CharacterArgument unique() {
        return new CharacterArgument(Type.UNIQUE);
    }

    public static CharacterArgument existing() {
        return new CharacterArgument(Type.EXISTING);
    }

    public static String getName(final CommandContext<?> context, final String name) {
        return NameArgument.getName(context, name);
    }

    public Type getType() {
        return type;
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var parsed = super.parse(reader);
        var storage = CharacterStorage.get();
        //TODO: change this error probably
        if (storage == null) throw UNKNOWN_ERROR.createWithContext(reader);
        var exists = storage.allCharacters().values().stream().anyMatch(character -> character.profile().name().equalsIgnoreCase(parsed));
        if (type == Type.EXISTING != exists) throw (type == Type.UNIQUE ? DUPLICATE : NOT_FOUND).create(parsed);
        return parsed;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (type == Type.UNIQUE) return builder.buildFuture();
        var storage = CharacterStorage.get();
        if (storage == null) return CompletableFuture.completedFuture(builder.build());
        SharedSuggestionProvider.suggest(storage.allCharacters().values().stream().map(character -> character.profile().name()), builder);
        return builder.buildFuture();
    }

    public enum Type {
        UNIQUE,
        EXISTING
    }

    public static class Serializer implements ArgumentTypeInfo<CharacterArgument, Serializer.Template> {
        public void serializeToNetwork(Serializer.Template template, FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeEnum(template.type);
        }

        public Serializer.Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
            Type stringType = friendlyByteBuf.readEnum(Type.class);
            return new Serializer.Template(stringType);
        }

        public void serializeToJson(Serializer.Template template, JsonObject jsonObject) {
            jsonObject.addProperty(
                "type", switch (template.type) {
                    case EXISTING -> "existing";
                    case UNIQUE -> "unique";
                }
            );
        }

        public Serializer.Template unpack(CharacterArgument type) {
            return new Serializer.Template(type.getType());
        }

        public final class Template implements ArgumentTypeInfo.Template<CharacterArgument> {
            final Type type;

            public Template(final Type stringType) {
                this.type = stringType;
            }

            @NotNull
            public CharacterArgument instantiate(@NotNull CommandBuildContext commandBuildContext) {
                return switch (this.type) {
                    case EXISTING -> CharacterArgument.existing();
                    case UNIQUE -> CharacterArgument.unique();
                };
            }

            @Override
            public ArgumentTypeInfo<CharacterArgument, ?> type() {
                return Serializer.this;
            }
        }
    }
}
