package com.chyzman.characteristic.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.world.item.component.ResolvableProfile;

public class ProfileArgument implements ArgumentType<ResolvableProfile> {

    public static ProfileArgument profile() {
        return new ProfileArgument();
    }

    public static ResolvableProfile getProfile(final com.mojang.brigadier.context.CommandContext<?> context, final String name) {
        return context.getArgument(name, ResolvableProfile.class);
    }

    @Override
    public ResolvableProfile parse(StringReader reader) throws CommandSyntaxException {
        try {
            return ResolvableProfile.createUnresolved(UuidArgument.uuid().parse(reader));
        } catch (CommandSyntaxException e) {
            return ResolvableProfile.createUnresolved(NameArgument.name().parse(reader));
        }
    }
}
