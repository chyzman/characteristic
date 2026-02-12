package com.chyzman.characteristic.cca;

import com.mojang.authlib.GameProfile;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.Owo;
import io.wispforest.owo.mixin.ServerCommonPacketListenerImplAccessor;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
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

    // Character UUID -> Character
    private final Map<UUID, Character> characters = new HashMap<>();

    // Player UUID -> Character UUID (and vice versa)
    private final Map<UUID, UUID> currentCharacters = new HashMap<>();

    // Connection -> Player Profile
    public final Map<Connection, GameProfile> connections = new WeakHashMap<>();

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

    private static final KeyedEndec<Set<Character>> CHARACTERS_ENDEC =
        Character.ENDEC.setOf()
            .keyed("characters", HashSet::new);

    private static final KeyedEndec<Map<UUID, UUID>> CURRENT_CHARACTERS_ENDEC =
        BuiltInEndecs.UUID
            .mapOf(UUID::toString, UUID::fromString)
            .keyed("current_characters", HashMap::new);


    public CharacterStorage(Scoreboard holder, @Nullable MinecraftServer server) {
        this.holder = holder;
        this.server = server;
    }

    @Override
    public void readData(ValueInput valueInput) {
        this.characters.clear();
        valueInput.get(CHARACTERS_ENDEC).forEach(this::initializeCharacter);

        this.currentCharacters.clear();
        this.currentCharacters.putAll(valueInput.get(CURRENT_CHARACTERS_ENDEC));
    }

    @Override
    public void writeData(ValueOutput valueOutput) {
        valueOutput.put(CHARACTERS_ENDEC, new HashSet<>(this.characters.values()));

        valueOutput.put(CURRENT_CHARACTERS_ENDEC, this.currentCharacters);
    }

    private void update() {
        CHARACTER_STORAGE.sync(holder);
    }

    //endregion

    public Character createCharacter(GameProfile profile, UUID owner) {
        var character = new Character(profile, owner);
        initializeCharacter(character);
        update();
        return character;
    }

    private void initializeCharacter(Character characteristics) {
        this.characters.put(characteristics.id(), characteristics);
    }

    public Map<UUID, Character> allCharacters() {
        return Collections.unmodifiableMap(characters);
    }

    public Map<UUID, UUID> currentCharacters() {
        return Collections.unmodifiableMap(currentCharacters);
    }

    @Nullable
    public Character getCharacter(GameProfile profile) {
        return characters.get(currentCharacters.get(profile.id()));
    }

    public void setCharacter(UUID target, UUID character) {
        currentCharacters.put(target, character);
        update();
    }

    public void clearCharacter(UUID target) {
        currentCharacters.remove(target);
        update();
    }

    @Nullable
    public GameProfile getControllingProfile(Connection connection) {
        return connections.get(connection);
    }

    @Nullable
    public GameProfile getControllingProfile(ServerCommonPacketListenerImpl serverCommonPacketListener) {
        return getControllingProfile(((ServerCommonPacketListenerImplAccessor) serverCommonPacketListener).owo$getConnection());
    }

    @Nullable
    public GameProfile getControllingProfile(ServerPlayer player) {
        return getControllingProfile(player.connection);
    }

    public static class Character {
        private final GameProfile profile;
        private final UUID owner;

        //region ENDEC STUFF

        public static final Endec<Character> ENDEC = StructEndecBuilder.of(
            CodecUtils.toEndec(ExtraCodecs.STORED_GAME_PROFILE.codec()).fieldOf("profile", s -> s.profile),
            BuiltInEndecs.UUID.fieldOf("owner", s -> s.owner),
            Character::new
        );

        private Character(GameProfile profile, UUID owner) {
            this.profile = profile;
            this.owner = owner;
        }

        //endregion

        //region GETTERS AND SETTERS

        public GameProfile profile() {
            return profile;
        }

        public UUID id() {
            return profile.id();
        }

        public String name() {
            return profile.name();
        }

        public UUID owner() {
            return owner;
        }

        //endregion

        public static Character forPlayer(GameProfile profile) {
            return new Character(profile, profile.id());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CharacterStorage storage)) return false;
        return Objects.equals(characters, storage.characters) && Objects.equals(currentCharacters, storage.currentCharacters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characters, currentCharacters);
    }
}
