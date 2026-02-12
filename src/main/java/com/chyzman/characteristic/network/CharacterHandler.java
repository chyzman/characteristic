package com.chyzman.characteristic.network;

import com.chyzman.characteristic.Characteristic;
import com.chyzman.characteristic.cca.CharacterStorage;
import com.chyzman.characteristic.mixin.client.access.ClientConfigurationPacketListenerImplAccessor;
import com.chyzman.characteristic.ui.widget.CharacterPicker;
import com.mojang.authlib.GameProfile;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.braid.core.BraidScreen;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CharacterHandler {


    public static void init() {
        PayloadTypeRegistry.configurationS2C().register(S2CSwapProfile.TYPE, CodecUtils.toPacketCodec(S2CSwapProfile.ENDEC));
        PayloadTypeRegistry.configurationC2S().register(C2SSwapProfile.TYPE, CodecUtils.toPacketCodec(C2SSwapProfile.ENDEC));
        PayloadTypeRegistry.configurationS2C().register(S2CPickCharacter.TYPE, CodecUtils.toPacketCodec(S2CPickCharacter.ENDEC));
        PayloadTypeRegistry.configurationC2S().register(C2SPickCharacter.TYPE, CodecUtils.toPacketCodec(C2SPickCharacter.ENDEC));
        PayloadTypeRegistry.playC2S().register(C2SOpenCharacterSwitcher.TYPE, CodecUtils.toPacketCodec(C2SOpenCharacterSwitcher.ENDEC));

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (!ServerConfigurationNetworking.canSend(handler, S2CPickCharacter.TYPE) || !ServerConfigurationNetworking.canSend(handler, S2CSwapProfile.TYPE))
                handler.disconnect(Component.translatableWithFallback(
                    "characteristic.multiplayer.disconnect.missing",
                    Language.getInstance().getOrDefault("multiplayer.disconnect.characteristic.missing")
                ));
            var storage = CharacterStorage.get();
            if (storage == null) return;
            var target = storage.getControllingProfile(handler);
            if (target == null) return;
            if (!storage.currentCharacters().containsKey(target.id())) {
                //TODO: validate choices once we actually make permission stuff
                var choices = storage.allCharacters().values().stream().toList();
                handler.addTask(new PickCharacterTask(choices));
            }
            handler.addTask(new SwapProfileTask(target));

        });

        ServerPlayNetworking.registerGlobalReceiver(
            C2SOpenCharacterSwitcher.TYPE, (payload, context) -> {
                var storage = CharacterStorage.get();
                if (storage == null) return;
                var listener = context.player().connection;
                var target = storage.getControllingProfile(listener);
                if (target == null) return;
                storage.clearCharacter(target.id());
                ServerPlayNetworking.reconfigure(listener);
            }
        );

        ServerConfigurationNetworking.registerGlobalReceiver(C2SSwapProfile.TYPE, (payload, context) -> context.networkHandler().completeTask(SwapProfileTask.TYPE));

        ServerConfigurationNetworking.registerGlobalReceiver(
            C2SPickCharacter.TYPE, (payload, context) -> {
                var handler = context.networkHandler();
                handler.completeTask(PickCharacterTask.TYPE);
                var storage = CharacterStorage.get();
                if (storage == null) return;
                var target = storage.getControllingProfile(handler);
                if (target == null) return;
                var character = storage.allCharacters().get(payload.choice());
                if (character == null) return;
                //TODO: validate character choice
                storage.setCharacter(target.id(), character.id());
//                handler.addTask(new SwapProfileTask(character.profile()));
            }
        );
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(
            S2CSwapProfile.TYPE,
            (payload, context) -> context.client().schedule(() -> {
                ((ClientConfigurationPacketListenerImplAccessor) context.networkHandler()).characteristic$setLocalGameProfile(payload.profile);
                context.responseSender().sendPacket(C2SSwapProfile.INSTANCE);
            })
        );

        ClientConfigurationNetworking.registerGlobalReceiver(
            S2CPickCharacter.TYPE,
            (payload, context) -> context.client().schedule(() -> context.client()
                .setScreen(new BraidScreen(new CharacterPicker(
                    payload.choices, character ->
                    context.responseSender().sendPacket(new C2SPickCharacter(character.id()))
                ))))
        );
    }

    //region PROFILE SWAPPING

    private static final Identifier SWAP_PROFILE = Characteristic.id("swap_profile");

    public record SwapProfileTask(GameProfile profile) implements ConfigurationTask {
        public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(SWAP_PROFILE.toString());

        @Override
        public void start(@NonNull Consumer<Packet<?>> consumer) {
            var storage = CharacterStorage.get();
            var target = profile;
            if (storage != null) {
                var id = storage.currentCharacters().getOrDefault(profile.id(), profile.id());
                if (id != null) {
                    var character = storage.allCharacters().get(id);
                    if (character != null) {
                        target = character.profile();
                    }
                }
            }
            consumer.accept(ServerConfigurationNetworking.createS2CPacket(new S2CSwapProfile(target)));
        }

        @Override
        public Type type() {
            return TYPE;
        }
    }

    public record S2CSwapProfile(GameProfile profile) implements CustomPacketPayload {
        public static final Type<S2CSwapProfile> TYPE = new Type<>(SWAP_PROFILE);
        public static final Endec<S2CSwapProfile> ENDEC = StructEndecBuilder.of(
            CodecUtils.toEndec(ExtraCodecs.STORED_GAME_PROFILE.codec()).fieldOf("profile", S2CSwapProfile::profile),
            S2CSwapProfile::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static class C2SSwapProfile implements CustomPacketPayload {
        public static final C2SSwapProfile INSTANCE = new C2SSwapProfile();
        public static final Type<C2SSwapProfile> TYPE = new Type<>(SWAP_PROFILE);
        public static final Endec<C2SSwapProfile> ENDEC = Endec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    //endregion

    //region CHARACTER PICKER

    private static final Identifier PICK_CHARACTER = Characteristic.id("pick_character");

    public record PickCharacterTask(List<CharacterStorage.Character> choices) implements ConfigurationTask {
        public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(PICK_CHARACTER.toString());

        @Override
        public void start(@NonNull Consumer<Packet<?>> consumer) {
            consumer.accept(ServerConfigurationNetworking.createS2CPacket(new S2CPickCharacter(choices)));
        }

        @Override
        public Type type() {
            return TYPE;
        }
    }

    public record S2CPickCharacter(List<CharacterStorage.Character> choices) implements CustomPacketPayload {
        public static final Type<S2CPickCharacter> TYPE = new Type<>(PICK_CHARACTER);
        public static final Endec<S2CPickCharacter> ENDEC = StructEndecBuilder.of(
            CharacterStorage.Character.ENDEC.listOf().fieldOf("choices", S2CPickCharacter::choices), S2CPickCharacter::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record C2SPickCharacter(UUID choice) implements CustomPacketPayload {
        public static final Type<C2SPickCharacter> TYPE = new Type<>(PICK_CHARACTER);
        public static final Endec<C2SPickCharacter> ENDEC = StructEndecBuilder.of(
            BuiltInEndecs.UUID.fieldOf("choice", C2SPickCharacter::choice),
            C2SPickCharacter::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static class C2SOpenCharacterSwitcher implements CustomPacketPayload {
        public static final C2SOpenCharacterSwitcher INSTANCE = new C2SOpenCharacterSwitcher();
        public static final Type<C2SOpenCharacterSwitcher> TYPE = new Type<>(Characteristic.id("request_character_switcher"));
        public static final Endec<C2SOpenCharacterSwitcher> ENDEC = Endec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    //endregion
}
