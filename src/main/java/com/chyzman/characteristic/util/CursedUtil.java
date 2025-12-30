package com.chyzman.characteristic.util;

import io.wispforest.owo.Owo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.Services;

@SuppressWarnings("resource")
public class CursedUtil {


    public static Services getServices() {
        var server = Owo.currentServer();
        if (server != null) return server.services();
        return getServicesClient();
    }

    @Environment(EnvType.CLIENT)
    private static Services getServicesClient() {
        return Minecraft.getInstance().services();
    }
}
