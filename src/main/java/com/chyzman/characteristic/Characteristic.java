package com.chyzman.characteristic;

import com.chyzman.characteristic.network.CharacterHandler;
import com.chyzman.characteristic.registry.CommandRegistry;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class Characteristic implements ModInitializer {
    public static final String MODID = "characteristic";
    public static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onInitialize() {
        CommandRegistry.init();

        CharacterHandler.init();
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
