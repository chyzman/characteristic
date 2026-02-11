package com.chyzman.characteristic.mixin;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin {

    @Shadow @Final protected Connection connection;

    @WrapOperation(
        method = "createCookie",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerCommonPacketListenerImpl;playerProfile()Lcom/mojang/authlib/GameProfile;"
        )
    )
    private GameProfile swapProfile(ServerCommonPacketListenerImpl instance, Operation<GameProfile> original) {
        var profile = original.call(instance);
        var storage = CharacterStorage.get();
        if (storage == null) return profile;
        return storage.getCharacter(storage.connections.getOrDefault(connection, profile)).profile();
    }
}
