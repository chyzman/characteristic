package com.chyzman.characteristic.mixin.client.access;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientConfigurationPacketListenerImpl.class)
public interface ClientConfigurationPacketListenerImplAccessor {
    @Accessor
    @Mutable
    void setLocalGameProfile(GameProfile gameProfile);
}
