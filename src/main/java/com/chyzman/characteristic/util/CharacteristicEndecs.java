package com.chyzman.characteristic.util;

import io.wispforest.endec.Endec;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.server.players.NameAndId;

public class CharacteristicEndecs {

    public static Endec<NameAndId> NAME_AND_ID = CodecUtils.toEndec(NameAndId.CODEC);
}
