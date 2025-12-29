package com.chyzman.characteristic.registry;

import com.chyzman.characteristic.cca.CharacterStorage;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.scoreboard.ScoreboardComponentInitializer;

import static com.chyzman.characteristic.Characteristic.id;

public class CCARegistry implements ScoreboardComponentInitializer {
    public static final ComponentKey<CharacterStorage> CHARACTER_STORAGE = ComponentRegistry.getOrCreate(id("storage"), CharacterStorage.class);

    @Override
    public void registerScoreboardComponentFactories(ScoreboardComponentFactoryRegistry registry) {
        registry.registerScoreboardComponent(CHARACTER_STORAGE, CharacterStorage::new);
    }
}
