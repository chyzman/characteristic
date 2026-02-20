package com.chyzman.characteristic.ui.widget;

import com.chyzman.characteristic.Characteristic;
import com.chyzman.characteristic.api.Character;
import com.chyzman.characteristic.api.CharacterProperties;
import com.chyzman.characteristic.network.CharacterHandler;
import com.chyzman.characteristic.util.BraidUtil;
import io.wispforest.owo.braid.animation.Easing;
import io.wispforest.owo.braid.core.*;
import io.wispforest.owo.braid.core.events.FilesDroppedEvent;
import io.wispforest.owo.braid.framework.BuildContext;
import io.wispforest.owo.braid.framework.proxy.WidgetState;
import io.wispforest.owo.braid.framework.widget.StatefulWidget;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.widgets.SpriteWidget;
import io.wispforest.owo.braid.widgets.animated.AnimatedAlign;
import io.wispforest.owo.braid.widgets.animated.AnimatedPadding;
import io.wispforest.owo.braid.widgets.basic.*;
import io.wispforest.owo.braid.widgets.button.Clickable;
import io.wispforest.owo.braid.widgets.button.MessageButton;
import io.wispforest.owo.braid.widgets.flex.*;
import io.wispforest.owo.braid.widgets.intents.Actions;
import io.wispforest.owo.braid.widgets.intents.Intent;
import io.wispforest.owo.braid.widgets.intents.Interactable;
import io.wispforest.owo.braid.widgets.intents.ShortcutTrigger;
import io.wispforest.owo.braid.widgets.label.Label;
import io.wispforest.owo.braid.widgets.scroll.VerticallyScrollable;
import io.wispforest.owo.braid.widgets.stack.Stack;
import io.wispforest.owo.braid.widgets.stack.StackBase;
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
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class CharacterPicker extends StatefulWidget {
    protected static final String TRANSLATION = "ui.characteristic.character_picker.";

    public static final Component TITLE = Component.translatable(TRANSLATION + "title");
    public static final Component ABORTED = Component.translatable(TRANSLATION + "aborted");
    public static final Component SELECT = Component.translatable(TRANSLATION + "select");

    public static final ListenableValue<List<Character>> CHARACTER_CHOICES = new ListenableValue<>(List.of());

    final ClientConfigurationNetworking.Context networkingContext;
    final List<Character> choices;
    final UUID owner;

    public CharacterPicker(
        ClientConfigurationNetworking.Context networkingContext,
        List<Character> choices,
        UUID owner
    ) {
        this.networkingContext = networkingContext;
        this.choices = choices;
        this.owner = owner;
    }

    @Override
    public WidgetState<CharacterPicker> createState() {
        return new State();
    }

    public static class State extends WidgetState<CharacterPicker> {
        private List<Character> choices;
        private boolean waitingForServer;
        private Character selected;
        private CharacterProperties selectedProperties;

        private boolean editing;
        private Character editingCharacter;
        private CharacterProperties editingProperties;
        private final TextEditingController nameInput = new TextEditingController();
        private final TextEditingController bioInput = new TextEditingController();

        private final Runnable choicesListener = () -> setState(() -> updateChoices(CHARACTER_CHOICES.value()));

        @Override
        public void init() {
            updateChoices(widget().choices);
            CHARACTER_CHOICES.addListener(choicesListener);
            nameInput.addListener(() -> {
                if (editingCharacter != null) setState(() -> editingCharacter = editingCharacter.name(nameInput.value().text()));
            });
            bioInput.addListener(() -> {
                if (editingCharacter != null) setState(() -> editingProperties.put(CharacterProperties.BIO_KEY, bioInput.value().text()));
            });
        }

        @Override
        public void dispose() {
            CHARACTER_CHOICES.removeListener(choicesListener);
        }

        private void updateChoices(List<Character> newChoices) {
            waitingForServer = false;
            choices = sort(newChoices);
            if (selected != null) select(choices.stream().filter(c -> c.id().equals(selected.id())).findFirst().orElse(null));
        }

        private void select(Character character) {
            selected = character;
            selectedProperties = selected != null ? CharacterProperties.fromProfile(selected.profile) : null;
            if (!canEdit()) editing = false;
            editingCharacter = selected != null ? new Character(selected) : null;
            editingProperties = selected != null ? CharacterProperties.fromProfile(selected.profile) : null;
            var newName = selected != null ? selected.name() : "";
            nameInput.setValue(new TextEditingValue(newName, TextSelection.collapsed(newName.length())));
            var newBio = editingProperties != null ? editingProperties.get(CharacterProperties.BIO_KEY) : "";
            bioInput.setValue(new TextEditingValue(newBio, TextSelection.collapsed(newBio.length())));
        }

        private List<Character> sort(List<Character> characters) {
            return characters.stream().sorted(Comparator.comparing(Character::name)).toList();
        }

        private boolean unsavedChanges() {
            return editing && (!Objects.equals(selected, editingCharacter) || !Objects.equals(selectedProperties, editingProperties));
        }

        private boolean canEdit() {
            return selected != null && selected.owner.equals(widget().owner);
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
                    waitingForServer || unsavedChanges() ? Map.of() : shortcuts,
                    widget -> widget.autoFocus(true).addCallbackAction(
                        SelectCharacterIntent.class,
                        (actionContext, intent) -> {
                            if (intent.index < choices.size()) setState(() -> select(choices.get(intent.index())));
                        }
                    ).addCallbackAction(
                        PickSelectedCharacterIntent.class, (actionContext, intent) -> {
                            if (selected != null) {
                                setState(() -> waitingForServer = true);
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
                        new Flexible(
                            new Stack(
                                new Align(
                                    Alignment.BOTTOM_RIGHT,
                                    new GliscoHatesMeAnimatedAlign(
                                        canEdit() ? Duration.ofSeconds(1) : Duration.ofMillis(250),
                                        Easing.IN_OUT_CUBIC,
                                        editing ? Alignment.of(3, 1) : Alignment.BOTTOM_RIGHT,
                                        1.5, null,
                                        new Padding(
                                            Insets.vertical(4),
                                            new Padding(
                                                Insets.left(-10),
                                                new ControlsOverride(
                                                    waitingForServer || selected == null,
                                                    new Stack(
                                                        Alignment.BOTTOM_RIGHT,
                                                        new StackBase(
                                                            new Panel(
                                                                Panel.VANILLA_LIGHT,
                                                                new Actions(
                                                                    widget -> widget
                                                                        .focusable(true)
                                                                        .skipTraversal(true)
                                                                        .addCallbackAction(SelectCharacterIntent.class, (actionContext, intent) -> {})
                                                                        .addCallbackAction(PickSelectedCharacterIntent.class, (actionContext, intent) -> {}),
                                                                    new Padding(
                                                                        Insets.all(4).withLeft(13),
                                                                        new Column(
                                                                            new VerticallyScrollable(
                                                                                new Column(
                                                                                    new Sized(
                                                                                        100, 16,
                                                                                        new TextBox(
                                                                                            nameInput,
                                                                                            //TODO: validate name input
                                                                                            widget -> widget.singleLine()
                                                                                        )
                                                                                    ),
                                                                                    new Sized(
                                                                                        100, null,
                                                                                        new TextBox(
                                                                                            bioInput,
                                                                                            //TODO: validate bio
                                                                                            widget -> {}
                                                                                        )
                                                                                    )
                                                                                )
                                                                            ),
                                                                            new Row(
                                                                                new MessageButton(
                                                                                    //TODO: translate
                                                                                    Component.literal("save"),
                                                                                    unsavedChanges(),
                                                                                    () -> {
                                                                                        setState(() -> waitingForServer = true);
                                                                                        editingProperties.applyToProfile(editingCharacter.profile);
                                                                                        widget().networkingContext
                                                                                            .responseSender()
                                                                                            .sendPacket(new CharacterHandler.C2SEditCharacter(editingCharacter));
                                                                                    }
                                                                                ),
                                                                                new MessageButton(
                                                                                    //TODO: translate
                                                                                    Component.literal("cancel"),
                                                                                    unsavedChanges(),
                                                                                    () -> setState(() -> select(selected))
                                                                                )
                                                                            )
                                                                        )
                                                                    )
                                                                )
                                                            )
                                                        ),
                                                        new Align(
                                                            Alignment.BOTTOM_RIGHT,
                                                            new GliscoHatesMeAnimatedAlign(
                                                                canEdit() ? Duration.ofMillis(500) : Duration.ofMillis(250),
                                                                Easing.IN_OUT_CUBIC,
                                                                canEdit() ? Alignment.of(5, 1) : Alignment.BOTTOM_RIGHT,
                                                                1.25, null,
                                                                new Padding(
                                                                    Insets.left(-3),
                                                                    new Stack(
                                                                        new SpriteWidget(
                                                                            Characteristic.id("edit_tab")
                                                                        ),
                                                                        new Padding(
                                                                            Insets.left(1),
                                                                            new Sized(
                                                                                Size.square(10),
                                                                                new Clickable(
                                                                                    () -> {
                                                                                        setState(() -> editing = !editing);
                                                                                        return true;
                                                                                    },
                                                                                    new SpriteWidget(
                                                                                        Characteristic.id("jerry")
                                                                                    )
                                                                                )
                                                                            )
                                                                        )
                                                                    )
                                                                )
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                ),
                                new ControlsOverride(
                                    waitingForServer || unsavedChanges(),
                                    new StackBase(
//                                            new Box(
//                                                Color.RED,
//                                                true,
                                        new Panel(
                                            Panel.VANILLA_LIGHT,
                                            new HitTestTrap(
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
                                                        selected == null
                                                            ? new Label(SELECT)
                                                            : new Column(
                                                                MainAxisAlignment.START,
                                                                CrossAxisAlignment.CENTER,
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
                                                                        setState(() -> waitingForServer = true);
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
                                                setState(() -> waitingForServer = true);
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
