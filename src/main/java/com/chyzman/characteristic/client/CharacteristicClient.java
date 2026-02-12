package com.chyzman.characteristic.client;

import com.chyzman.characteristic.network.CharacterHandler;
import net.fabricmc.api.ClientModInitializer;

public class CharacteristicClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CharacterHandler.initClient();
    }
}
