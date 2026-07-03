package com.chyzman.characteristic.mixin.client;

import com.chyzman.characteristic.client.CharacteristicClient;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Options;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.SocketAddress;

@Environment(EnvType.CLIENT)
@Mixin(Options.class)
public abstract class OptionsMixin {

    @Inject(method = "processOptions", at = @At("RETURN"))
    private void saveCharacteristicOptions(Options.FieldAccess fieldAccess, CallbackInfo ci) {
        fieldAccess.process(CharacteristicClient.ALWAYS_PICK_CHARACTER_KEY, CharacteristicClient.ALWAYS_PICK_CHARACTER);
    }
}
