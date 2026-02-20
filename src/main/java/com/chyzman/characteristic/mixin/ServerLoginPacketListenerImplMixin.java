package com.chyzman.characteristic.mixin;

import com.chyzman.characteristic.api.Character;
import com.chyzman.characteristic.cca.CharacterStorage;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {
    @Shadow @Final Connection connection;

    @Inject(method = "finishLoginAndWaitForClient", at = @At("HEAD"))
    private void storeWhoThisIs(GameProfile profile, CallbackInfo ci) {
        var storage = CharacterStorage.get();
        if (storage == null) return;
        storage.connections.put(connection, profile);
        if (storage.allCharacters().containsKey(profile.id())) return;
        storage.setCharacter(profile.id(), Character.forPlayer(profile).id());
    }

    @WrapOperation(
        method = "verifyLoginAndFinishConnectionSetup",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/players/PlayerList;disconnectAllPlayersWithProfile(Ljava/util/UUID;)Z"
        )
    )
    private boolean ignoreDuplicateProfiles$verifyLoginAndFinishConnectionSetup(PlayerList instance, UUID uUID, Operation<Boolean> original) {
        return false;
    }
}
