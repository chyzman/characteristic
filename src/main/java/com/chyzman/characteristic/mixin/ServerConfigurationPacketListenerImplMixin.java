package com.chyzman.characteristic.mixin;

import com.chyzman.characteristic.network.SwitchGameProfileS2CPayload;
import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin extends ServerCommonPacketListenerImpl {
    @Shadow @Final private GameProfile gameProfile;

    public ServerConfigurationPacketListenerImplMixin(MinecraftServer minecraftServer, Connection connection, CommonListenerCookie commonListenerCookie) {
        super(minecraftServer, connection, commonListenerCookie);
    }

    @Inject(method = "returnToWorld", at = @At("HEAD"))
    private void sendNewProfile(CallbackInfo ci) {
        send(ServerConfigurationNetworking.createS2CPacket(new SwitchGameProfileS2CPayload(gameProfile)));
    }
}
