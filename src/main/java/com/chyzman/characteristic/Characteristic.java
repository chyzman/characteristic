package com.chyzman.characteristic;

import com.chyzman.characteristic.network.SwitchGameProfileS2CPayload;
import com.chyzman.characteristic.registry.CommandRegistry;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

public class Characteristic implements ModInitializer {
    public static final String MODID = "characteristic";

    @Override
    public void onInitialize() {
        CommandRegistry.init();

        PayloadTypeRegistry.configurationS2C().register(SwitchGameProfileS2CPayload.TYPE, CodecUtils.toPacketCodec(SwitchGameProfileS2CPayload.ENDEC));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
