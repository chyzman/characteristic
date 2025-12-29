package com.chyzman.characteristic.mixin;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CommonListenerCookie.class)
public abstract class CommonListenerCookieMixin {

    @WrapOperation(
        method = "createInitial",
        at = @At(
            value = "NEW",
            target = "(Lcom/mojang/authlib/GameProfile;ILnet/minecraft/server/level/ClientInformation;Z)Lnet/minecraft/server/network/CommonListenerCookie;"
        )
    )
    private static CommonListenerCookie swapPlayer(
        GameProfile gameProfile,
        int latency,
        ClientInformation clientInformation,
        boolean transferred,
        Operation<CommonListenerCookie> original
    ) {
        var storage = CharacterStorage.get();
        return original.call(
            storage.getCharacteristics(gameProfile).character(),
            latency,
            clientInformation,
            transferred
        );
    }
}
