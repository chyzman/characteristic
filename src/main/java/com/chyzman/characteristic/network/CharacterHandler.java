package com.chyzman.characteristic.network;

import com.chyzman.characteristic.Characteristic;
import com.chyzman.characteristic.api.Character;
import com.chyzman.characteristic.cca.CharacterStorage;
import com.chyzman.characteristic.client.CharacteristicClient;
import com.chyzman.characteristic.mixin.client.access.ClientConfigurationPacketListenerImplAccessor;
import com.chyzman.characteristic.ui.CharacteristicConfigurationScreen;
import com.chyzman.characteristic.ui.widget.CharacterPicker;
import com.mojang.authlib.GameProfile;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.mixin.ServerCommonPacketListenerImplAccessor;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.locale.Language;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.util.ExtraCodecs;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;

public class CharacterHandler {

    public static void init() {
        PayloadTypeRegistry.configurationS2C().register(S2CSwapProfile.TYPE, CodecUtils.toPacketCodec(S2CSwapProfile.ENDEC));
        PayloadTypeRegistry.configurationC2S().register(C2SSwapProfile.TYPE, CodecUtils.toPacketCodec(C2SSwapProfile.ENDEC));

        PayloadTypeRegistry.configurationS2C().register(S2CPickCharacter.TYPE, CodecUtils.toPacketCodec(S2CPickCharacter.ENDEC));
        PayloadTypeRegistry.configurationC2S().register(C2SPickCharacter.TYPE, CodecUtils.toPacketCodec(C2SPickCharacter.ENDEC));
        PayloadTypeRegistry.configurationS2C().register(S2CUpdateCharacterChoices.TYPE, CodecUtils.toPacketCodec(S2CUpdateCharacterChoices.ENDEC));
        PayloadTypeRegistry.configurationC2S().register(C2SEditCharacter.TYPE, CodecUtils.toPacketCodec(C2SEditCharacter.ENDEC));
        PayloadTypeRegistry.playC2S().register(C2SOpenCharacterSwitcher.TYPE, CodecUtils.toPacketCodec(C2SOpenCharacterSwitcher.ENDEC));

        PayloadTypeRegistry.configurationS2C().register(S2CCheckSwap.TYPE, CodecUtils.toPacketCodec(S2CCheckSwap.ENDEC));
        PayloadTypeRegistry.configurationC2S().register(C2SCheckSwap.TYPE, CodecUtils.toPacketCodec(C2SCheckSwap.ENDEC));

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (
                !ServerConfigurationNetworking.canSend(handler, S2CPickCharacter.TYPE) ||
                !ServerConfigurationNetworking.canSend(handler, S2CSwapProfile.TYPE) ||
                !ServerConfigurationNetworking.canSend(handler, S2CCheckSwap.TYPE)
            )
                handler.disconnect(Component.translatableWithFallback(
                    "characteristic.multiplayer.disconnect.missing",
                    Language.getInstance().getOrDefault("multiplayer.disconnect.characteristic.missing")
                ));
            var storage = CharacterStorage.get();
            if (storage == null) return;
            var target = storage.getControllingProfile(handler);
            if (target == null) return;
            handler.addTask(new CheckSwapTask());
            //TODO: somehow make the current character for each account be the first logged in one instead of the last
            if (
                SWAP_WANTERS.contains(((ServerCommonPacketListenerImplAccessor) handler).owo$getConnection()) ||
                !storage.currentCharacters().containsKey(target.id()) ||
                server.getPlayerList().getPlayer(storage.currentCharacters().get(target.id())) != null
            ) {
                //TODO: filter choices once we actually make permission stuff
                var choices = storage.allCharacters().values().stream().toList();
                handler.addTask(new PickCharacterTask(choices, target.id()));
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

        ServerConfigurationNetworking.registerGlobalReceiver(
            C2SSwapProfile.TYPE, (payload, context) -> context.networkHandler().completeTask(SwapProfileTask.TYPE)
        );

        ServerConfigurationNetworking.registerGlobalReceiver(
            C2SPickCharacter.TYPE, (payload, context) -> {
                var handler = context.networkHandler();
                var storage = CharacterStorage.get();
                if (storage != null) {
                    var target = storage.getControllingProfile(handler);
                    if (target != null) {
                        var character = storage.allCharacters().get(payload.choice());
                        if (character != null) {
                            //TODO: validate character choice
                            storage.setCharacter(target.id(), character.id());
                        }
                    }
                }
                handler.completeTask(PickCharacterTask.TYPE);
            }
        );

        ServerConfigurationNetworking.registerGlobalReceiver(
            C2SEditCharacter.TYPE, (payload, context) -> {
                var storage = CharacterStorage.get();
                if (storage != null) {
                    var target = storage.getControllingProfile(context.networkHandler());
                    if (target != null) {
                        var character = storage.allCharacters().get(payload.character.id());
                        if (character != null) {
                            var permission = character.permissions.getOrDefault(target.id(), null);
                            if (character.differencesAreValidEdits(payload.character, permission)) storage.modifyCharacter(payload.character);
                        }
                    }
                }
                //TODO: filter choices once we have permissions
                context.responseSender().sendPacket(new S2CUpdateCharacterChoices(storage.allCharacters().values().stream().toList()));
            }
        );

        ServerConfigurationNetworking.registerGlobalReceiver(
            C2SCheckSwap.TYPE, (payload, context) -> {
                if (payload.wantsToSwap()) SWAP_WANTERS.add(((ServerCommonPacketListenerImplAccessor) context.networkHandler()).owo$getConnection());
                else SWAP_WANTERS.remove(((ServerCommonPacketListenerImplAccessor) context.networkHandler()).owo$getConnection());
                context.networkHandler().completeTask(CheckSwapTask.TYPE);
            }
        );
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(
            S2CSwapProfile.TYPE,
            (payload, context) -> {
                ((ClientConfigurationPacketListenerImplAccessor) context.networkHandler()).characteristic$setLocalGameProfile(payload.profile);
                context.responseSender().sendPacket(C2SSwapProfile.INSTANCE);
            }
        );

        ClientConfigurationNetworking.registerGlobalReceiver(
            S2CPickCharacter.TYPE,
            (payload, context) -> context.client().schedule(() -> {
                var client = context.client();
                client.setScreen(
                    new CharacteristicConfigurationScreen(client.screen, new CharacterPicker(context, payload.choices, payload.owner)));
            })
        );

        ClientConfigurationNetworking.registerGlobalReceiver(
            S2CUpdateCharacterChoices.TYPE,
            (payload, context) -> CharacterPicker.CHARACTER_CHOICES.setValue(payload.choices())
        );

        ClientConfigurationNetworking.registerGlobalReceiver(
            S2CCheckSwap.TYPE,
            (payload, context) ->
                context.responseSender().sendPacket(new C2SCheckSwap(CharacteristicClient.ALWAYS_PICK_CHARACTER.get()))
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
                    if (character != null) target = character.profile;
                }
            }
            consumer.accept(ServerConfigurationNetworking.createS2CPacket(new S2CSwapProfile(target)));
        }

        @Override
        @NonNull
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
        @NonNull
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static class C2SSwapProfile implements CustomPacketPayload {
        public static final C2SSwapProfile INSTANCE = new C2SSwapProfile();
        public static final Type<C2SSwapProfile> TYPE = new Type<>(SWAP_PROFILE);
        public static final Endec<C2SSwapProfile> ENDEC = Endec.unit(INSTANCE);

        @Override
        @NonNull
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    //endregion

    //region CHARACTER PICKER

    private static final Identifier PICK_CHARACTER = Characteristic.id("pick_character");

    public record PickCharacterTask(List<Character> choices, UUID owner) implements ConfigurationTask {
        public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(PICK_CHARACTER.toString());

        @Override
        public void start(@NonNull Consumer<Packet<?>> consumer) {
            consumer.accept(ServerConfigurationNetworking.createS2CPacket(new S2CPickCharacter(choices, owner)));
        }

        @Override
        @NonNull
        public Type type() {
            return TYPE;
        }
    }

    public record S2CPickCharacter(List<Character> choices, UUID owner) implements CustomPacketPayload {
        public static final Type<S2CPickCharacter> TYPE = new Type<>(PICK_CHARACTER);
        public static final Endec<S2CPickCharacter> ENDEC = StructEndecBuilder.of(
            Character.ENDEC.listOf().fieldOf("choices", S2CPickCharacter::choices),
            BuiltInEndecs.UUID.fieldOf("owner", S2CPickCharacter::owner),
            S2CPickCharacter::new
        );

        @Override
        @NonNull
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
        @NonNull
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static class C2SOpenCharacterSwitcher implements CustomPacketPayload {
        public static final C2SOpenCharacterSwitcher INSTANCE = new C2SOpenCharacterSwitcher();
        public static final Type<C2SOpenCharacterSwitcher> TYPE = new Type<>(Characteristic.id("request_character_switcher"));
        public static final Endec<C2SOpenCharacterSwitcher> ENDEC = Endec.unit(INSTANCE);

        @Override
        @NonNull
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record S2CUpdateCharacterChoices(List<Character> choices) implements CustomPacketPayload {
        public static final Type<S2CUpdateCharacterChoices> TYPE = new Type<>(Characteristic.id("update_character_choices"));
        public static final Endec<S2CUpdateCharacterChoices> ENDEC = StructEndecBuilder.of(
            Character.ENDEC.listOf().fieldOf("choices", S2CUpdateCharacterChoices::choices),
            S2CUpdateCharacterChoices::new
        );

        @Override
        @NonNull
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record C2SEditCharacter(Character character) implements CustomPacketPayload {
        public static final Type<C2SEditCharacter> TYPE = new Type<>(Characteristic.id("edit_character"));
        public static final Endec<C2SEditCharacter> ENDEC = StructEndecBuilder.of(
            Character.ENDEC.fieldOf("edited", C2SEditCharacter::character),
            C2SEditCharacter::new
        );

        @Override
        @NonNull
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    //endregion

    //region CHECK SWAP

    private static final Identifier CHECK_SWAP = Characteristic.id("check_client_swap");

    private static final Set<Connection> SWAP_WANTERS = Collections.newSetFromMap(new WeakHashMap<>());

    public record CheckSwapTask() implements ConfigurationTask {
        public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(CHECK_SWAP.toString());

        @Override
        public void start(@NonNull Consumer<Packet<?>> consumer) {
            consumer.accept(ServerConfigurationNetworking.createS2CPacket(S2CCheckSwap.INSTANCE));
        }

        @Override
        @NonNull
        public Type type() {
            return TYPE;
        }
    }

    public static class S2CCheckSwap implements CustomPacketPayload {
        public static final S2CCheckSwap INSTANCE = new S2CCheckSwap();
        public static final Type<S2CCheckSwap> TYPE = new Type<>(CHECK_SWAP);
        public static final Endec<S2CCheckSwap> ENDEC = Endec.unit(INSTANCE);

        @Override
        @NonNull
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }


    public record C2SCheckSwap(boolean wantsToSwap) implements CustomPacketPayload {
        public static final Type<C2SCheckSwap> TYPE = new Type<>(CHECK_SWAP);
        public static final Endec<C2SCheckSwap> ENDEC = StructEndecBuilder.of(
            Endec.BOOLEAN.fieldOf("wantsToSwap", C2SCheckSwap::wantsToSwap),
            C2SCheckSwap::new
        );

        @Override
        @NonNull
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    //endregion
}
