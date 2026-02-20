package com.chyzman.characteristic.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.server.IntegratedPlayerList;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.notifications.NotificationService;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;

import java.net.SocketAddress;

@Environment(EnvType.CLIENT)
@Mixin(IntegratedPlayerList.class)
public abstract class IntegratedPlayerListMixin extends PlayerList {

    public IntegratedPlayerListMixin(
        MinecraftServer minecraftServer,
        LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess,
        PlayerDataStorage playerDataStorage,
        NotificationService notificationService
    ) {
        super(minecraftServer, layeredRegistryAccess, playerDataStorage, notificationService);
    }

    @WrapMethod(method = "canPlayerLogin")
    private Component ignoreDuplicateProfiles$canPlayerLogin(SocketAddress socketAddress, NameAndId nameAndId, Operation<Component> original) {
        return super.canPlayerLogin(socketAddress, nameAndId);
    }
}
