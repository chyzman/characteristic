package com.chyzman.characteristic.network;

import com.chyzman.characteristic.Characteristic;
import com.mojang.authlib.GameProfile;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ExtraCodecs;

public record SwitchGameProfileS2CPayload(GameProfile newProfile) implements CustomPacketPayload {
    public static final Type<SwitchGameProfileS2CPayload> TYPE = new Type<>(Characteristic.id("switch_game_profile"));
    public static final Endec<SwitchGameProfileS2CPayload> ENDEC = StructEndecBuilder.of(
        CodecUtils.toEndec(ExtraCodecs.STORED_GAME_PROFILE.codec()).fieldOf("new_profile", SwitchGameProfileS2CPayload::newProfile),
        SwitchGameProfileS2CPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
