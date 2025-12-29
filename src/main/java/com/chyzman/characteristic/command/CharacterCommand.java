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

import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CharacterCommand {
    private static final SimpleCommandExceptionType UNKNOWN_ERROR = new SimpleCommandExceptionType(Component.translatable("commands.characteristic.failed"));

    private static final DynamicCommandExceptionType DUPLICATE_CHARACTER = new DynamicCommandExceptionType(o -> Component.translatable(
        "commands.characteristic.character.failed.duplicate",
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

            );
        });
    }

    private static int createCharacter(
        CommandSourceStack source,
        String name
    ) throws CommandSyntaxException {
        var storage = CharacterStorage.get();
        if (storage == null) throw UNKNOWN_ERROR.create();
        if (storage.allCharacters().values().stream().anyMatch(profile -> profile.name().equalsIgnoreCase(name))) throw DUPLICATE_CHARACTER.create(name);
        var data = storage.getCharacteristics(source.getPlayerOrException().getGameProfile());

        var server = source.getServer();


        Util.nonCriticalIoPool()
            .execute(
                () -> {
                    Optional<GameProfile> optional = server.services().profileResolver().fetchById(data.owner());
                    server.execute(
                        () -> optional.ifPresentOrElse(
                            playerProfile -> {
                                storage.setCharacter(playerProfile, storage.createCharacter(playerProfile, name).id());
                                source.sendSuccess(() -> Component.translatable("commands.characteristic.character.create.success"), false);
                            },
                            // This should never happen
                            () -> source.sendFailure(Component.translatable("commands.characteristic.failed"))
                        )
                    );
                }
            );
        return 1;
    }
}
