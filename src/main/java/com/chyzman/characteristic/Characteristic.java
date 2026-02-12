package com.chyzman.characteristic;

import com.chyzman.characteristic.network.CharacterHandler;
import com.chyzman.characteristic.registry.CommandRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class Characteristic implements ModInitializer {
    public static final String MODID = "characteristic";

    @Override
    public void onInitialize() {
        CommandRegistry.init();

        CharacterHandler.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
