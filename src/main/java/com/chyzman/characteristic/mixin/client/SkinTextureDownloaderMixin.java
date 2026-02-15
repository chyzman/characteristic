package com.chyzman.characteristic.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SkinTextureDownloader;
import net.minecraft.util.FileUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Base64;

@Mixin(SkinTextureDownloader.class)
public abstract class SkinTextureDownloaderMixin {

    @Shadow @Final private static Logger LOGGER;

    @Inject(
        method = "downloadSkin",
        at = @At(
            value = "INVOKE",
            target = "Ljava/net/URI;create(Ljava/lang/String;)Ljava/net/URI;"
        ),
        cancellable = true
    )
    private void supportDataUris(
        Path path,
        String string,
        CallbackInfoReturnable<NativeImage> cir
    ) throws IOException {
        if ("data".equalsIgnoreCase(URI.create(string).getScheme())) {
            var comma = string.indexOf(',');
            if (comma == -1) throw new IOException("Invalid data URL: " + string);
            var meta = string.substring(5, comma);
            var dataPart = string.substring(comma + 1);
            byte[] bytes;
            if (meta.contains(";base64")) {
                bytes = Base64.getDecoder().decode(dataPart);
            } else {
                bytes = URLDecoder
                    .decode(dataPart, StandardCharsets.ISO_8859_1)
                    .getBytes(StandardCharsets.ISO_8859_1);
            }

            try {
                FileUtil.createDirectoriesSafe(path.getParent());
                Files.write(path, bytes);
            } catch (IOException var14) {
                LOGGER.warn("Failed to cache texture {} in {}", string, path);
            }

            cir.setReturnValue(NativeImage.read(bytes));
        }
    }

}
