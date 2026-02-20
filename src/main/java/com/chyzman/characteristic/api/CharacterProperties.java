package com.chyzman.characteristic.api;

import com.chyzman.characteristic.Characteristic;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class CharacterProperties {
    public static final Logger LOGGER = LoggerFactory.getLogger(CharacterProperties.class);

    public static final Identifier BIO_KEY = Characteristic.id("bio");

    private static final Identifier ID = Characteristic.id("character_properties");
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    private final Map<String, String> properties = new HashMap<>();

    private CharacterProperties() {}

    public static CharacterProperties fromProfile(GameProfile profile) {
        var properties = new CharacterProperties();
        var property = Iterables.getFirst(profile.properties().get(ID.toString()), null);
        if (property == null) return properties;
        try {
            var json = GSON.fromJson(new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8), JsonObject.class);
            json.asMap().forEach((key, value) -> {
                if (value.isJsonPrimitive()) properties.properties.put(key, value.getAsString());
            });
        } catch (final JsonParseException | IllegalArgumentException e) {
            LOGGER.error("Could not decode character properties", e);
        }
        return properties;
    }

    public void applyToProfile(GameProfile profile) {
        profile.properties().removeAll(ID.toString());
        if (properties.isEmpty()) return;
        var json = GSON.toJsonTree(properties).getAsJsonObject();
        var encoded = Base64.getEncoder().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
        profile.properties().put(ID.toString(), new Property(ID.toString(), encoded));
    }

    public String get(Identifier key) {
        return properties.getOrDefault(key.toString(), "");
    }

    public CharacterProperties put(Identifier key, String value) {
        if (value == null || value.isBlank()) {
            properties.remove(key.toString());
            return this;
        }
        properties.put(key.toString(), value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CharacterProperties that)) return false;
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(properties);
    }
}
