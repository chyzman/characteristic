package com.chyzman.characteristic.ui;

import io.wispforest.owo.braid.core.BraidScreen;
import io.wispforest.owo.braid.framework.widget.Widget;
import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Path;
import java.util.List;

public class CharacteristicConfigurationScreen extends BraidScreen {
    private final Screen parent;

    public CharacteristicConfigurationScreen(Screen parent, Widget rootWidget) {
        super(rootWidget);
        this.parent = parent;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onFilesDrop(List<Path> list) {
        super.onFilesDrop(list);
    }
}
