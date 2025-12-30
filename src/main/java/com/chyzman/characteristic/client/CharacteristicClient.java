package com.chyzman.characteristic.client;

import com.chyzman.characteristic.mixin.client.access.ClientConfigurationPacketListenerImplAccessor;
import com.chyzman.characteristic.network.SwitchGameProfileS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;

public class CharacteristicClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(SwitchGameProfileS2CPayload.TYPE, (payload, context) -> {
            ((ClientConfigurationPacketListenerImplAccessor) context.networkHandler()).setLocalGameProfile(payload.newProfile());
        });
    }
}
