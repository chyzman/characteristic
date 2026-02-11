package com.chyzman.characteristic.ui.widget;

import com.chyzman.characteristic.cca.CharacterStorage;
import io.wispforest.owo.braid.framework.BuildContext;
import io.wispforest.owo.braid.framework.widget.StatelessWidget;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.widgets.button.MessageButton;
import io.wispforest.owo.braid.widgets.flex.Column;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

public class CharacterPicker extends StatelessWidget {
    final List<CharacterStorage.Character> choices;
    final Consumer<CharacterStorage.Character> onPick;

    public CharacterPicker(
        List<CharacterStorage.Character> choices,
        Consumer<CharacterStorage.Character> onPick
    ) {
        this.choices = choices;
        this.onPick = onPick;
    }

    @Override
    public Widget build(BuildContext context) {
        return new Column(
            choices.stream().map(CharacterEntry::new).toList()
        );
    }

    public class CharacterEntry extends StatelessWidget {
        final CharacterStorage.Character character;

        public CharacterEntry(CharacterStorage.Character character) {
            this.character = character;
        }

        @Override
        public Widget build(BuildContext context) {
            return new MessageButton(
                Component.literal(character.name()),
                () -> {
                    onPick.accept(character);
                    Minecraft.getInstance().setScreen(null);
                }
            );
        }
    }
}
