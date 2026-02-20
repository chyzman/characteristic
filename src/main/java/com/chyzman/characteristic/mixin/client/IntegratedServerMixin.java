package com.chyzman.characteristic.mixin.client;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {

    @ModifyExpressionValue(method = "isSingleplayerOwner", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/server/IntegratedServer;getSingleplayerProfile()Lcom/mojang/authlib/GameProfile;"))
    private GameProfile characterizeProfile(GameProfile original) {
        var storage = CharacterStorage.get();
        if (storage == null) return original;
        var character = storage.getCharacterFromPlayer(original.id());
        return character == null ? original : character.profile;
    }
}
