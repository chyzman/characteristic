package com.chyzman.characteristic.registry;

import com.chyzman.characteristic.command.CharacterCommand;
import com.chyzman.characteristic.command.argument.CharacterArgument;
import com.chyzman.characteristic.command.argument.NameArgument;
import com.chyzman.characteristic.command.argument.ProfileArgument;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;

import static com.chyzman.characteristic.Characteristic.id;

public class CommandRegistry {
    public static void init() {
        ArgumentTypeRegistry.registerArgumentType(id("name"), NameArgument.class, SingletonArgumentInfo.contextFree(NameArgument::new));
        ArgumentTypeRegistry.registerArgumentType(id("character"), CharacterArgument.class, new CharacterArgument.Serializer());
        ArgumentTypeRegistry.registerArgumentType(id("profile"), ProfileArgument.class, SingletonArgumentInfo.contextFree(ProfileArgument::new));

        CharacterCommand.register();
    }
}
