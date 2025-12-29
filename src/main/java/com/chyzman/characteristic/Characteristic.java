package com.chyzman.characteristic;

import com.chyzman.characteristic.command.CharacterCommand;
import com.chyzman.characteristic.network.SwitchGameProfileS2CPayload;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.ResourceLocation;

public class Characteristic implements ModInitializer {
    public static final String MODID = "characteristic";

    @Override
    public void onInitialize() {
        CharacterCommand.register();

        PayloadTypeRegistry.configurationS2C().register(SwitchGameProfileS2CPayload.TYPE, CodecUtils.toPacketCodec(SwitchGameProfileS2CPayload.ENDEC));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
