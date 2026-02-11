package com.chyzman.characteristic.client;

import com.chyzman.characteristic.mixin.client.access.ClientConfigurationPacketListenerImplAccessor;
import com.chyzman.characteristic.network.CharacterSwitchHandler;
import com.chyzman.characteristic.network.payload.S2CSwitchGameProfilePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;

public class CharacteristicClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(
            S2CSwitchGameProfilePayload.TYPE,
            (payload, context) -> ((ClientConfigurationPacketListenerImplAccessor) context.networkHandler()).characteristic$setLocalGameProfile(payload.newProfile())
        );

        CharacterSwitchHandler.initClient();
    }
}
