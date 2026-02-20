package com.chyzman.characteristic.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Unique private static boolean usedSinglePlayerData = false;

    @WrapOperation(
        method = "loadPlayerData",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;isSingleplayerOwner(Lnet/minecraft/server/players/NameAndId;)Z"
        )
    )
    private static boolean onlyUseSinglePlayerDataOnce(MinecraftServer instance, NameAndId nameAndId, Operation<Boolean> original) {
        var returned = usedSinglePlayerData && original.call(instance, nameAndId);
        usedSinglePlayerData = true;
        return returned;
    }
}
