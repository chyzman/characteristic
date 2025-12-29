package com.chyzman.characteristic.cca;

import com.mojang.authlib.GameProfile;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.Owo;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.*;

import static com.chyzman.characteristic.registry.CCARegistry.CHARACTER_STORAGE;

public class CharacterStorage implements Component, AutoSyncedComponent {
    private final Scoreboard holder;
    public final @Nullable MinecraftServer server;

    private final Map<UUID, GameProfile> characters = new HashMap<>();

    private final Map<UUID, Characteristics> player2Data = new HashMap<>();
    private final Map<UUID, Characteristics> character2Data = new HashMap<>();

    //region CURSED GETTER

    @Nullable
    @SuppressWarnings("resource")
    public static CharacterStorage get() {
        var server = Owo.currentServer();
        if (server != null) return CHARACTER_STORAGE.getNullable(server.getScoreboard());
        return getClient();
    }

    @Environment(EnvType.CLIENT)
    @Nullable
    private static CharacterStorage getClient() {
        var world = Minecraft.getInstance().level;
        if (world == null) return null;
        return CHARACTER_STORAGE.getNullable(world.getScoreboard());
    }

    //endregion

    //region ENDEC STUFF

    private static final KeyedEndec<Set<GameProfile>> CHARACTERS_ENDEC = CodecUtils.toEndec(ExtraCodecs.STORED_GAME_PROFILE.codec()).setOf().keyed("characters", HashSet::new);

    private static final KeyedEndec<Set<Characteristics>> CHARACTERISTICS_ENDEC = Characteristics.ENDEC.setOf().keyed("characteristics", HashSet::new);

    public CharacterStorage(Scoreboard holder, @Nullable MinecraftServer server) {
        this.holder = holder;
        this.server = server;
    }

    @Override
    public void readData(ValueInput valueInput) {
        var characterSet = valueInput.get(CHARACTERS_ENDEC);
        this.characters.clear();
        for (var character : characterSet) this.characters.put(character.id(), character);

        var characteristicsSet = valueInput.get(CHARACTERISTICS_ENDEC);
        this.player2Data.clear();
        this.character2Data.clear();
        for (var data : characteristicsSet) {
            player2Data.put(data.owner, data);
            for (var character : data.characters) character2Data.put(character, data);
        }
    }

    @Override
    public void writeData(ValueOutput valueOutput) {
        valueOutput.put(CHARACTERS_ENDEC, new HashSet<>(this.characters.values()));

        valueOutput.put(CHARACTERISTICS_ENDEC, new HashSet<>(this.player2Data.values()));
    }

    private void update() {
        CHARACTER_STORAGE.sync(holder);
    }

    //endregion

    //region INITIALIZATION

    private Characteristics characterizePlayer(GameProfile profile) {
        var player = profile.id();

        characters.put(player, profile);

        var characteristics = Characteristics.forPlayer(player);

        initializeCharacteristics(characteristics);

        update();

        return characteristics;
    }

    private void initializeCharacteristics(Characteristics characteristics) {
        player2Data.put(characteristics.owner, characteristics);
        for (var character : characteristics.characters) {
            character2Data.put(character, characteristics);
        }
    }

    //endregion

    public Map<UUID, GameProfile> allCharacters() {
        return Collections.unmodifiableMap(characters);
    }

    @Nullable
    public Characteristics getCharacteristics(GameProfile profile) {
        if (!characters.containsKey(profile.id())) return characterizePlayer(profile);
        return character2Data.get(profile.id());
    }

    public GameProfile createCharacter(GameProfile playerProfile, String name) {
        var character = new GameProfile(UUID.randomUUID(), name, playerProfile.properties());
        characters.put(character.id(), character);
        var data = player2Data.get(playerProfile.id());
        data.characters.add(character.id());
        character2Data.put(character.id(), data);
        update();
        return character;
    }

    public void setCharacter(GameProfile playerProfile, UUID character) {
        var data = getCharacteristics(playerProfile);
        if (data == null) return;
        data.character(character);
        update();
    }

    public static class Characteristics {
        private final UUID owner;
        private UUID character;
        private final Set<UUID> characters;

        //region ENDEC STUFF

        public static final Endec<Characteristics> ENDEC = StructEndecBuilder.of(
            BuiltInEndecs.UUID.fieldOf("owner", s -> s.owner),
            BuiltInEndecs.UUID.fieldOf("character", s -> s.character),
            BuiltInEndecs.UUID.setOf().fieldOf("characters", s -> s.characters),
            Characteristics::new
        );

        private Characteristics(UUID owner, UUID character, Set<UUID> characters) {
            this.owner = owner;
            this.character = character;
            this.characters = characters;
            if (character != null) this.characters.add(character);
        }

        //endregion

        //region GETTERS AND SETTERS

        public UUID owner() {
            return owner;
        }

        public Characteristics character(UUID character) {
            this.character = character;
            this.characters.add(character);
            return this;
        }

        public GameProfile character() {
            return CharacterStorage.get().characters.get(this.character);
        }

        public Set<UUID> characters() {
            return Collections.unmodifiableSet(characters);
        }

        //endregion

        public static Characteristics forPlayer(UUID player) {
            return new Characteristics(player, player, new HashSet<>(Set.of()));
        }
    }

    @Override
    public boolean isRequiredOnClient() {
        return false;
    }
}
