package com.chyzman.characteristic.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;

import java.util.Arrays;
import java.util.Collection;

public class NameArgument implements ArgumentType<String> {
    private static final Collection<String> EXAMPLES = Arrays.asList("steve", "chyzman", "Esper", "amogus");

    protected static final Dynamic2CommandExceptionType TOO_LONG = new Dynamic2CommandExceptionType((size, max) -> Component.translatable("argument.characteristic.name.too_long", size, max));
    protected static final DynamicCommandExceptionType INVALID_CHARACTER = new DynamicCommandExceptionType((invalid_char) -> Component.translatable("argument.characteristic.name.invalid_character", invalid_char));
    protected static final SimpleCommandExceptionType EMPTY = new SimpleCommandExceptionType(Component.translatable("argument.characteristic.name.empty"));

    public static final int MAX_LENGTH = 16;

    public static NameArgument name() {
        return new NameArgument();
    }

    public static String getName(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        var start = reader.getCursor();
        var name = new StringBuilder();
        for (int i = 0; i < MAX_LENGTH; i++) {
            if (!reader.canRead() || reader.peek() == ' ') break;
            var c = reader.peek();
            if (!StringUtil.isValidPlayerName(Character.toString(c))) throw INVALID_CHARACTER.create(c);
            name.append(c);
            reader.skip();
        }
        if (reader.canRead() && reader.peek() != ' ') {
            while (reader.canRead() && reader.peek() != ' ') reader.skip();
            throw TOO_LONG.create(reader.getCursor() - start, MAX_LENGTH);
        }
        if (name.isEmpty()) throw EMPTY.create();
        return name.toString();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
