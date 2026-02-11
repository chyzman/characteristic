package com.chyzman.characteristic.ui.widget;

import com.chyzman.characteristic.Characteristic;
import io.wispforest.owo.braid.core.Alignment;
import io.wispforest.owo.braid.core.Size;
import io.wispforest.owo.braid.framework.BuildContext;
import io.wispforest.owo.braid.framework.proxy.WidgetState;
import io.wispforest.owo.braid.framework.widget.StatefulWidget;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.util.layers.LayerAlignment;
import io.wispforest.owo.braid.widgets.SpriteWidget;
import io.wispforest.owo.braid.widgets.basic.Align;
import io.wispforest.owo.braid.widgets.basic.Padding;
import io.wispforest.owo.braid.widgets.basic.Sized;
import io.wispforest.owo.braid.widgets.button.Button;
import io.wispforest.owo.braid.widgets.stack.Stack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

import java.time.Duration;

public class InventoryLayer extends StatefulWidget {
    private static final Identifier JERRY = Characteristic.id("jerry");

    @Override
    public WidgetState<?> createState() {
        return new State();
    }

    public static class State extends WidgetState<InventoryLayer> {

        @Override
        public void init() {
            this.update(Duration.ZERO);
        }

        private void update(Duration delta) {
            this.setState(() -> {});
            this.scheduleAnimationCallback(this::update);
        }

        @Override
        public Widget build(BuildContext context) {
            var screen = Minecraft.getInstance().screen;
            var creative = screen instanceof CreativeModeInventoryScreen;
            return LayerAlignment.atContainerScreenCoordinates(
                creative ? 181 : 77, creative ? 4 : 7,
                (creative && ((CreativeModeInventoryScreen)screen).getSelectedItemGroup().getType() != CreativeModeTab.Type.INVENTORY) ?
                    new Padding(Size.zero()) :
                    new Sized(
                        10, 10,
                        new Stack(
                            new Button(
                                () -> Minecraft.getInstance().player.displayClientMessage(Component.literal("amogus"), false),
                                null
                            ),
                            new SpriteWidget(JERRY)
                        )
                    )
            );
        }
    }
}
