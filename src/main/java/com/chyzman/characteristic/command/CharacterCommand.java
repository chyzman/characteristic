package com.chyzman.characteristic.command;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.chyzman.characteristic.command.argument.CharacterArgument;
import com.chyzman.characteristic.command.argument.ProfileArgument;
import com.chyzman.characteristic.mixin.client.access.ServerCommonPacketListenerImplAccessor;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class CharacterCommand {
    public static final SimpleCommandExceptionType UNKNOWN_ERROR = new SimpleCommandExceptionType(Component.translatable("commands.characteristic.failed"));

    private static final DynamicCommandExceptionType DUPLICATE_CHARACTER = new DynamicCommandExceptionType(o -> Component.translatable(
        "argument.characteristic.character.duplicate",
        o
    ));

    private static final DynamicCommandExceptionType CHARACTER_NOT_FOUND = new DynamicCommandExceptionType(o -> Component.translatable(
        "argument.characteristic.character.not_found",
        o
    ));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, commandSelection) -> {
            dispatcher.register(literal("character")
                    .then(literal("create")
                        .then(argument("name", CharacterArgument.unique())
                            .executes(context ->
                                createCharacter(
                                    context.getSource(),
                                    new GameProfile(UUID.randomUUID(), CharacterArgument.getName(context, "name")),
                                    null
                                )
                            )
                        )
                        .then(literal("from-profile")
                            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                            .then(argument("profile", ProfileArgument.profile())
                                .executes(context -> {
                                    var source = context.getSource();
                                    ProfileArgument.getProfile(context, "profile").resolveProfile(context.getSource().getServer().services().profileResolver())
                                        .whenComplete((profile, throwable) -> {
                                            if (throwable != null) {
                                                source.sendFailure(Component.literal(throwable.getMessage()));
                                                return;
                                            }
                                            try {
                                                createCharacter(context.getSource(), profile, profile.id());
                                            } catch (CommandSyntaxException e) {
                                                source.sendFailure(Component.literal(e.getMessage()));
                                            }
                                        });
                                    return 1;
                                })
                            )
                        )
                    )
                    .then(literal("set")
                            .then(argument("name", CharacterArgument.existing())
//                            .suggests((context, builder) -> {
//                                var storage = CharacterStorage.get();
//                                if (storage == null) return builder.buildFuture();
//                                var data = storage.getCharacteristics(context.getSource().getPlayerOrException().getGameProfile());
//                                if (data == null) return builder.buildFuture();
//                                data.characters().stream()
//                                    .filter(uuid -> !uuid.equals(data.character().id()))
//                                    .map(uuid -> storage.allCharacters().get(uuid))
//                                    .filter(Objects::nonNull)
//                                    .map(GameProfile::name)
//                                    .forEach(builder::suggest);
//                                return builder.buildFuture();
//                            })
                                    .executes(context ->
                                        setCharacter(
                                            context.getSource(),
                                            CharacterArgument.getName(context, "name")
                                        )
                                    )
                            )
                    )
                    .then(literal("debug")
                        .then(literal("dump")
                            .then(literal("list")
                                .executes(context -> {
                                    var storage = storageOrThrow();
                                    storage.allCharacters().values().forEach(character -> context.getSource().sendSuccess(
                                        () -> Component.literal(
                                            character.name() + "\n" +
                                            character.id() + "\n" +
                                            character.owner()
                                        ), false
                                    ));
                                    return 1;
                                })
                            )
                            .then(literal("selected")
                                .executes(context -> {
                                    var storage = storageOrThrow();
                                    storage.currentCharacters().forEach((player, character) ->
                                        context.getSource().sendSuccess(() -> Component.literal(player + " → " + storage.allCharacters().get(character).name()), false));
                                    return 1;
                                })
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

    private static int createCharacter(CommandSourceStack source, GameProfile profile, @Nullable UUID owner) throws CommandSyntaxException {
        var server = source.getServer();
        var storage = storageOrThrow();
        if (storage.allCharacters().values().stream().anyMatch(character -> character.name().equalsIgnoreCase(profile.name())))
            throw DUPLICATE_CHARACTER.create(profile.name());
        var player = source.getPlayerOrException();
        var target = storage.getControllingProfile(player);
        if (target == null) throw UNKNOWN_ERROR.create();
        if (owner == null) owner = target.id();
        storage.setCharacter(target.id(), storage.createCharacter(profile, owner).id());
        source.sendSuccess(() -> Component.translatable("commands.characteristic.character.create.success", profile.name()), false);
        ServerPlayNetworking.reconfigure(player);
        return 1;
    }

    private static int setCharacter(CommandSourceStack source, String name) throws CommandSyntaxException {
        var storage = storageOrThrow();
        var player = source.getPlayerOrException();
        var target = storage.getControllingProfile(player);
        if (target == null) throw UNKNOWN_ERROR.create();
        var targetCharacter = storage.allCharacters().values().stream()//            .filter(Objects::nonNull)
            .filter(character -> character.name().equalsIgnoreCase(name))
            .findFirst();
        if (targetCharacter.isEmpty()) throw CHARACTER_NOT_FOUND.create(name);
        storage.setCharacter(target.id(), targetCharacter.get().id());
        source.sendSuccess(() -> Component.translatable("commands.characteristic.character.set.success", player.getDisplayName(), name), false);
        ServerPlayNetworking.reconfigure(player);
        return 1;
    }
}
