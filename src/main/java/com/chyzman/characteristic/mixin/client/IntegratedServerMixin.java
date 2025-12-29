package com.chyzman.characteristic.mixin.client;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {

    @ModifyVariable(method = "isSingleplayerOwner", at = @At(value = "HEAD"), argsOnly = true)
    private NameAndId getActualOwner(NameAndId value) {
        var storage = CharacterStorage.get();
        if (storage == null) return value;
        //TODO: fix this gameprofile not including skin information
        var data = storage.getCharacteristics(new GameProfile(value.id(), value.name()));
        if (data == null) return value;
        return new NameAndId(data.character());
    }
}
