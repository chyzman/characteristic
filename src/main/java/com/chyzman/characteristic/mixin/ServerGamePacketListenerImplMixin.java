package com.chyzman.characteristic.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl {

    public ServerGamePacketListenerImplMixin(
        MinecraftServer minecraftServer,
        Connection connection,
        CommonListenerCookie commonListenerCookie
    ) {
        super(minecraftServer, connection, commonListenerCookie);
    }

    @Inject(method = "handleConfigurationAcknowledged", at = @At("RETURN"))
    private void addReconnect(ServerboundConfigurationAcknowledgedPacket packet, CallbackInfo ci) {
        server.execute(() -> ((ServerConfigurationPacketListenerImpl) connection.getPacketListener()).returnToWorld());
    }
}
