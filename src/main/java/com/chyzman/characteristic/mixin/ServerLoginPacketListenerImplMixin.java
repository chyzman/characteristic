package com.chyzman.characteristic.mixin;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.mojang.authlib.GameProfile;
import io.wispforest.owo.mixin.ServerCommonPacketListenerImplAccessor;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {
    @Shadow @Final Connection connection;

    @Inject(method = "finishLoginAndWaitForClient", at = @At("HEAD"))
    private void storeWhoThisIs(GameProfile profile, CallbackInfo ci) {
        var storage = CharacterStorage.get();
        if (storage == null) return;
        storage.connections.put(connection, profile);
        if (storage.allCharacters().containsKey(profile.id())) return;
        storage.setCharacter(profile.id(), CharacterStorage.Character.forPlayer(profile).id());
    }
}
