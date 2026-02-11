package com.chyzman.characteristic.client;

import com.chyzman.characteristic.mixin.client.access.ClientConfigurationPacketListenerImplAccessor;
import com.chyzman.characteristic.network.SwitchGameProfileS2CPayload;
import com.chyzman.characteristic.ui.widget.InventoryLayer;
import io.wispforest.owo.braid.util.layers.BraidLayersBinding;
import io.wispforest.owo.braid.widgets.button.Button;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public class CharacteristicClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(
            SwitchGameProfileS2CPayload.TYPE,
            (payload, context) -> ((ClientConfigurationPacketListenerImplAccessor) context.networkHandler()).characteristic$setLocalGameProfile(payload.newProfile())
        );

        BraidLayersBinding.add(screen -> screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen, new InventoryLayer());
    }
}
