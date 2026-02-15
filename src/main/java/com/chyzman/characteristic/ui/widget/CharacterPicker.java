package com.chyzman.characteristic.ui.widget;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.chyzman.characteristic.network.CharacterHandler;
import com.chyzman.characteristic.util.BraidUtil;
import com.mojang.authlib.GameProfile;
import io.wispforest.owo.braid.core.Alignment;
import io.wispforest.owo.braid.core.Insets;
import io.wispforest.owo.braid.core.ListenableValue;
import io.wispforest.owo.braid.framework.BuildContext;
import io.wispforest.owo.braid.framework.proxy.WidgetState;
import io.wispforest.owo.braid.framework.widget.StatefulWidget;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.widgets.basic.*;
import io.wispforest.owo.braid.widgets.button.MessageButton;
import io.wispforest.owo.braid.widgets.flex.*;
import io.wispforest.owo.braid.widgets.intents.Actions;
import io.wispforest.owo.braid.widgets.intents.Intent;
import io.wispforest.owo.braid.widgets.intents.Interactable;
import io.wispforest.owo.braid.widgets.intents.ShortcutTrigger;
import io.wispforest.owo.braid.widgets.label.Label;
import io.wispforest.owo.braid.widgets.scroll.VerticallyScrollable;
import io.wispforest.owo.braid.widgets.sharedstate.SharedState;
import io.wispforest.owo.braid.widgets.textinput.TextBox;
import io.wispforest.owo.braid.widgets.textinput.TextEditingController;
import io.wispforest.owo.braid.widgets.textinput.TextEditingValue;
import io.wispforest.owo.braid.widgets.textinput.TextSelection;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CharacterPicker extends StatefulWidget {
    protected static final String TRANSLATION = "ui.characteristic.character_picker.";

    public static final Component TITLE = Component.translatable(TRANSLATION + "title");
    public static final Component ABORTED = Component.translatable(TRANSLATION + "aborted");
    public static final Component SELECT = Component.translatable(TRANSLATION + "select");

    public static final ListenableValue<List<CharacterStorage.Character>> CHARACTER_CHOICES = new ListenableValue<>(List.of());

    final ClientConfigurationNetworking.Context networkingContext;
    final List<CharacterStorage.Character> choices;

    public CharacterPicker(
        ClientConfigurationNetworking.Context networkingContext,
        List<CharacterStorage.Character> choices
    ) {
        this.networkingContext = networkingContext;
        this.choices = choices;
    }

    @Override
    public WidgetState<CharacterPicker> createState() {
        return new State();
    }

    public static class State extends WidgetState<CharacterPicker> {
        private List<CharacterStorage.Character> choices;
        private boolean madeSelection;
        private CharacterStorage.Character selected;
        private TextEditingController nameInput = new TextEditingController();

        private final Runnable choicesListener = () -> setState(() -> updateChoices(CHARACTER_CHOICES.value()));

        @Override
        public void init() {
            updateChoices(widget().choices);
            CHARACTER_CHOICES.addListener(choicesListener);
        }

        @Override
        public void dispose() {
            CHARACTER_CHOICES.removeListener(choicesListener);
        }

        private void select(CharacterStorage.Character character) {
            selected = character;
            var newName = selected != null ? selected.name() : "";
            nameInput.setValue(new TextEditingValue(newName, TextSelection.collapsed(newName.length())));
        }

        private void updateChoices(List<CharacterStorage.Character> newChoices) {
            choices = sort(newChoices);
            if (selected != null) {
                selected = choices.stream().filter(c -> c.id().equals(selected.id())).findFirst().orElse(null);
                select(selected);
            }
        }

        private List<CharacterStorage.Character> sort(List<CharacterStorage.Character> characters) {
            return characters.stream().sorted(Comparator.comparing(CharacterStorage.Character::name)).toList();
        }

        @Override
        public Widget build(BuildContext context) {
            Map<List<ShortcutTrigger>, Intent> shortcuts = BraidUtil.NUMBERS
                .stream()
                .limit(choices.size())
                .collect(Collectors.toMap(List::of, trigger -> new SelectCharacterIntent(BraidUtil.NUMBERS.indexOf(trigger))));
            shortcuts.put(List.of(BraidUtil.ENTER), new PickSelectedCharacterIntent());
            return new Center(
                new Interactable(
                    madeSelection ? Map.of() : shortcuts,
                    widget -> widget.autoFocus(true).addCallbackAction(
                        SelectCharacterIntent.class,
                        (actionContext, intent) -> {
                            if (intent.index < choices.size()) setState(() -> select(choices.get(intent.index())));
                        }
                    ).addCallbackAction(
                        PickSelectedCharacterIntent.class, (actionContext, intent) -> {
                            if (selected != null) {
                                setState(() -> madeSelection = true);
                                widget().networkingContext.responseSender().sendPacket(new CharacterHandler.C2SPickCharacter(selected.id()));
                            }
                        }
                    ),
                    new Column(
                        MainAxisAlignment.START,
                        CrossAxisAlignment.CENTER,
                        new Sized(
                            null, HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT,
                            new Align(
                                Alignment.CENTER,
                                new Label(TITLE)
                            )
                        ),
                        new ControlsOverride(
                            madeSelection,
                            new Flexible(
                                new Panel(
                                    Panel.VANILLA_LIGHT,
                                    new Padding(
                                        Insets.all(6),
                                        new Row(
                                            new Panel(
                                                Panel.VANILLA_INSET,
                                                new Padding(
                                                    Insets.all(1),
                                                    new VerticallyScrollable(
                                                        new IntrinsicWidth(
                                                            new Column(
                                                                MainAxisAlignment.START,
                                                                CrossAxisAlignment.CENTER,
                                                                choices.stream()
                                                                    .map(character ->
                                                                        new MessageButton(
                                                                            Component.literal(character.name()),
                                                                            !character.equals(selected),
                                                                            () -> setState(() -> select(character))
                                                                        )).toList()
                                                            )
                                                        )
                                                    )
                                                )
                                            ),
                                            new Padding(Insets.all(2)),
                                            selected == null ?
                                                new Label(SELECT) :
                                                new Column(
                                                    MainAxisAlignment.START,
                                                    CrossAxisAlignment.CENTER,
                                                    new Sized(
                                                        null, 16,
                                                        new Row(
                                                            new Sized(
                                                                100, null,
                                                                new Actions(
                                                                    widget -> widget
                                                                        .focusable(false)
                                                                        .skipTraversal(true)
                                                                        .addCallbackAction(SelectCharacterIntent.class, (actionContext, intent) -> {})
                                                                        .addCallbackAction(PickSelectedCharacterIntent.class, (actionContext, intent) -> {}),
                                                                    new TextBox(
                                                                        nameInput,
                                                                        //TODO: validate name input
                                                                        widget -> widget.singleLine()
                                                                    )
                                                                )
                                                            ),
                                                            new MessageButton(
                                                                Component.literal("e"),
                                                                () -> {
                                                                    var modified = new CharacterStorage.Character(
                                                                        new GameProfile(selected.id(), nameInput.value().text(), selected.profile().properties()),
                                                                        selected.owner()
                                                                    );
                                                                    widget().networkingContext.responseSender().sendPacket(new CharacterHandler.C2SEditCharacter(modified));
                                                                }
                                                            )
                                                        )

                                                    ),
                                                    new Sized(
                                                        100, 200,
                                                        new CursedCharacterWidget(
                                                            1,
                                                            selected,
                                                            widget -> widget.displayMode(CursedCharacterWidget.DisplayMode.CURSOR)
                                                        )
                                                    ),
                                                    new MessageButton(
                                                        SELECT,
                                                        () -> {
                                                            setState(() -> madeSelection = true);
                                                            widget().networkingContext
                                                                .responseSender()
                                                                .sendPacket(new CharacterHandler.C2SPickCharacter(selected.id()));
                                                        }
                                                    )
                                                )
                                        )
                                    )
                                )
                            )
                        ),
                        new Sized(
                            null, HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT,
                            new Align(
                                Alignment.CENTER,
                                new DelayedEnabler(
                                    Duration.ofSeconds(10),
                                    new Sized(
                                        Button.DEFAULT_WIDTH, Button.DEFAULT_HEIGHT,
                                        new MessageButton(
                                            CommonComponents.GUI_DISCONNECT,
                                            () -> {
                                                setState(() -> madeSelection = true);
                                                widget().networkingContext.responseSender().disconnect(CharacterPicker.ABORTED);
                                                Minecraft.getInstance().setScreen(null);
                                            }
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            );
        }
    }

    public record SelectCharacterIntent(int index) implements Intent {}

    public record PickSelectedCharacterIntent() implements Intent {}

}
