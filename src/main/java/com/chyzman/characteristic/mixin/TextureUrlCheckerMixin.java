package com.chyzman.characteristic.mixin;

import com.mojang.authlib.yggdrasil.TextureUrlChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureUrlChecker.class)
public abstract class TextureUrlCheckerMixin {

    @Inject(
        method = "isAllowedTextureDomain",
        at = @At(
            value = "INVOKE",
            target = "Ljava/net/URI;getScheme()Ljava/lang/String;"
        ),
        cancellable = true
    )
    private static void allowDataUri(
        String url,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (url.startsWith("data:image/")) cir.setReturnValue(true);
    }
}
