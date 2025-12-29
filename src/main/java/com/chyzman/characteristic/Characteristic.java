package com.chyzman.characteristic;

import com.chyzman.characteristic.command.CharacterCommand;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

public class Characteristic implements ModInitializer {
    public static final String MODID = "characteristic";

    @Override
    public void onInitialize() {
        CharacterCommand.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
