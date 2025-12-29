package com.chyzman.characteristic.mixin;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.network.CommonListenerCookie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CommonListenerCookie.class)
public abstract class CommonListenerCookieMixin {

    @ModifyVariable(method = "<init>", at = @At(value = "HEAD"), argsOnly = true)
    private static GameProfile swapPlayer(
        GameProfile value
    ) {
        var storage = CharacterStorage.get();
        return storage.getCharacteristics(value).character();
    }
}
