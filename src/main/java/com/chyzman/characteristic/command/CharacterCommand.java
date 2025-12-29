package com.chyzman.characteristic.command;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CharacterCommand {
    private static final SimpleCommandExceptionType UNKNOWN_ERROR = new SimpleCommandExceptionType(Component.translatable("commands.characteristic.failed"));

    private static final DynamicCommandExceptionType DUPLICATE_CHARACTER = new DynamicCommandExceptionType(o -> Component.translatable(
        "commands.characteristic.character.failed.duplicate",
        o
    ));

    private static final DynamicCommandExceptionType CHARACTER_NOT_FOUND = new DynamicCommandExceptionType(o -> Component.translatable(
        "commands.characteristic.character.failed.not_found",
        o
    ));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, commandSelection) -> {
            dispatcher.register(
                literal("character")
                    .then(literal("create")
                        .then(argument("name", StringArgumentType.word())
                            .executes(context ->
                                createCharacter(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "name")
                                )
                            )
                        )
                    )
                    .then(literal("set")
                        .then(argument("name", StringArgumentType.word())
                            .suggests((context, builder) -> {
                                var storage = CharacterStorage.get();
                                if (storage == null) return builder.buildFuture();
                                var data = storage.getCharacteristics(context.getSource().getPlayerOrException().getGameProfile());
                                if (data == null) return builder.buildFuture();
                                data.characters().stream()
                                    .filter(uuid -> !uuid.equals(data.character().id()))
                                    .map(uuid -> storage.allCharacters().get(uuid))
                                    .filter(Objects::nonNull)
                                    .map(GameProfile::name)
                                    .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(context ->
                                setCharacter(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "name")
                                )
                            )
                        )
                    )
            );
        });
    }

    private static CharacterStorage storageOrThrow() throws CommandSyntaxException {
        var storage = CharacterStorage.get();
        if (storage == null) throw UNKNOWN_ERROR.create();
        return storage;
    }

    private static int createCharacter(CommandSourceStack source, String name) throws CommandSyntaxException {
        var server = source.getServer();
        var storage = storageOrThrow();
        if (storage.allCharacters().values().stream().anyMatch(profile -> profile.name().equalsIgnoreCase(name))) throw DUPLICATE_CHARACTER.create(name);
        var target = source.getPlayerOrException();
        var data = storage.getCharacteristics(target.getGameProfile());
        if (data == null) throw UNKNOWN_ERROR.create();

        Util.nonCriticalIoPool()
            .execute(
                () -> {
                    Optional<GameProfile> optional = server.services().profileResolver().fetchById(data.owner());
                    server.execute(
                        () -> optional.ifPresentOrElse(
                            playerProfile -> {
                                storage.setCharacter(playerProfile, storage.createCharacter(playerProfile, name).id());
                                source.sendSuccess(() -> Component.translatable("commands.characteristic.character.create.success", name), false);
                                target.connection.switchToConfig();
                            },
                            // This should never happen
                            () -> source.sendFailure(Component.translatable("commands.characteristic.failed"))
                        )
                    );
                }
            );
        return 1;
    }

    private static int setCharacter(CommandSourceStack source, String characterName) throws CommandSyntaxException {
        var storage = storageOrThrow();
        var target = source.getPlayerOrException();
        var data = storage.getCharacteristics(target.getGameProfile());
        if (data == null) throw UNKNOWN_ERROR.create();
        var targetCharacter = data.characters().stream()
            .map(uuid -> storage.allCharacters().get(uuid))
            .filter(Objects::nonNull)
            .filter(profile -> profile.name().equalsIgnoreCase(characterName))
            .findFirst();
        if (targetCharacter.isEmpty()) throw CHARACTER_NOT_FOUND.create(characterName);
        storage.setCharacter(target.getGameProfile(), targetCharacter.get().id());
        source.sendSuccess(() -> Component.translatable("commands.characteristic.character.set.success", target.getDisplayName(), characterName), false);
        target.connection.switchToConfig();

        return 1;
    }
}
