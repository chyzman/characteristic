package com.chyzman.characteristic.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import org.jetbrains.annotations.Nullable;

public class ProfileUtil {

    public static GameProfile with(
        GameProfile profile,
        @Nullable String name,
        @Nullable PropertyMap properties
    ) {
        return new GameProfile(
            profile.id(),
            name != null ? name : profile.name(),
            properties != null ? properties : profile.properties()
        );
    }

    public static GameProfile withName(GameProfile profile, String name) {
        return with(profile, name, null);
    }
}
