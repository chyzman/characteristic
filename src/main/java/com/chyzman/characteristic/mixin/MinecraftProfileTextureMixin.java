package com.chyzman.characteristic.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.net.URI;

@Mixin(MinecraftProfileTexture.class)
public abstract class MinecraftProfileTextureMixin {

    @Shadow @Final private String url;

    @WrapMethod(method = "getHash")
    private String supportDataUriHash(
        Operation<String> original
    ) {
        if (url.startsWith("data:image/")) return url.hashCode() + "";
        return original.call();
    }
}
