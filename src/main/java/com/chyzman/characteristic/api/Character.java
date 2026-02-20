package com.chyzman.characteristic.api;

import com.chyzman.characteristic.util.ProfileUtil;
import com.mojang.authlib.GameProfile;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Character {
    public GameProfile profile;
    public final UUID owner;
    public final Map<UUID, Permission> permissions;
    @Nullable public UUID syncParent;

    //region ENDEC STUFF

    public static final Endec<Character> ENDEC = StructEndecBuilder.of(
        CodecUtils.toEndec(ExtraCodecs.STORED_GAME_PROFILE.codec()).fieldOf("profile", s -> s.profile),
        BuiltInEndecs.UUID.fieldOf("owner", s -> s.owner),
        Endec.forEnum(Permission.class).mapOf(UUID::toString, UUID::fromString).fieldOf("permissions", s -> s.permissions),
        BuiltInEndecs.UUID.nullableFieldOf("syncParent", s -> s.syncParent, true),
        Character::new
    );

    //endregion

    //region CONSTRUCTORS

    public Character(GameProfile profile, UUID owner, Map<UUID, Permission> permissions, @Nullable UUID syncParent) {
        this.profile = profile;
        this.owner = owner;
        this.permissions = permissions;
        this.syncParent = syncParent;
    }

    public Character(GameProfile profile, UUID owner) {
        this(profile, owner, new HashMap<>(), null);
    }

    public Character(Character other) {
        this(other.profile, other.owner, other.permissions, other.syncParent);
    }

    public static Character forPlayer(GameProfile profile) {
        return new Character(profile, profile.id(), new HashMap<>(), null);
    }

    //endregion

    //region GETTERS AND SETTERS

    public UUID id() {
        return profile.id();
    }

    public String name() {
        return profile.name();
    }

    public Character name(String name) {
        this.profile = ProfileUtil.withName(profile, name);
        return this;
    }

    //endregion

    //region EDITING

    public boolean differencesAreValidEdits(Character other, Permission permission) {
        if (!permission.canEdit()) return false;

        if (!this.id().equals(other.id())) return false;
        if (!this.owner.equals(other.owner)) return false;
        return true;
    }

    //endregion

    //region EQUALITY

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Character character)) return false;
        if (!Objects.equals(profile, character.profile)) return false;
        if (!Objects.equals(owner, character.owner)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(profile, owner);
    }

    //endregion

    public enum Permission {
        USE(1),
        EDIT(2),
        FULL(3);

        private final int level;

        Permission(int level) {
            this.level = level;
        }

        public boolean canEdit() {
            return this.level >= EDIT.level;
        }

        public boolean isFull() {
            return this.level >= FULL.level;
        }
    }
}
