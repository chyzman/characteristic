package com.chyzman.characteristic.client;

import com.chyzman.characteristic.Characteristic;
import com.chyzman.characteristic.network.CharacterHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.OptionInstance;

public class CharacteristicClient implements ClientModInitializer {

    public static final String ALWAYS_PICK_CHARACTER_KEY = Characteristic.MODID + ".alwaysPickCharacter";

    public static final OptionInstance<Boolean> ALWAYS_PICK_CHARACTER = OptionInstance.createBoolean(
        "options." + ALWAYS_PICK_CHARACTER_KEY,
        false
    );

    @Override
    public void onInitializeClient() {
        CharacterHandler.initClient();
    }
}
