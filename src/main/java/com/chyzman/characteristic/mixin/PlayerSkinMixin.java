package com.chyzman.characteristic.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerSkin.class)
public abstract class PlayerSkinMixin {

    @Shadow @Final @Mutable private boolean secure;

    @Inject(
        method = "<init>",
        at = @At(value = "RETURN")
    )
    private void secureSkins(
        ClientAsset.Texture texture,
        ClientAsset.Texture texture2,
        ClientAsset.Texture texture3,
        PlayerModelType playerModelType,
        boolean bl,
        CallbackInfo ci
    ) {
        this.secure = true;
    }
}
