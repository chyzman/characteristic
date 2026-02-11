package com.chyzman.characteristic.network;

import com.chyzman.characteristic.Characteristic;
import com.chyzman.characteristic.cca.CharacterStorage;
import com.chyzman.characteristic.ui.widget.CharacterPicker;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.braid.core.BraidScreen;
import io.wispforest.owo.serialization.CodecUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.ConfigurationTask;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CharacterSwitchHandler {

    public static void init() {
        PayloadTypeRegistry.configurationS2C().register(RequestCharacterChoice.TYPE, CodecUtils.toPacketCodec(RequestCharacterChoice.ENDEC));
        PayloadTypeRegistry.configurationC2S().register(PickCharacter.TYPE, CodecUtils.toPacketCodec(PickCharacter.ENDEC));
        PayloadTypeRegistry.playC2S().register(RequestCharacterSwitcher.TYPE, CodecUtils.toPacketCodec(RequestCharacterSwitcher.ENDEC));

        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (!ServerConfigurationNetworking.canSend(handler, RequestCharacterChoice.TYPE)) handler.disconnect(Component.translatableWithFallback(
                "characteristic.multiplayer.disconnect.missing",
                Language.getInstance().getOrDefault("multiplayer.disconnect.characteristic.missing")
            ));
            var storage = CharacterStorage.get();
            if (storage == null) return;
            var target = storage.getControllingProfile(handler);
            if (target == null) return;
            if (storage.currentCharacters().containsKey(target.id())) return;
            //TODO: validate choices once we actually make permission stuff
            var choices = storage.allCharacters().values().stream().toList();
            handler.addTask(new PickCharacterTask(choices));
        });

        ServerPlayNetworking.registerGlobalReceiver(
            RequestCharacterSwitcher.TYPE, (payload, context) -> {
                var storage = CharacterStorage.get();
                if (storage == null) return;
                var connection = context.player().connection;
                var target = storage.getControllingProfile(connection);
                if (target == null) return;
                storage.clearCharacter(target.id());
                connection.switchToConfig();
            }
        );

        ServerConfigurationNetworking.registerGlobalReceiver(
            PickCharacter.TYPE, (payload, context) -> {
                context.networkHandler().completeTask(PickCharacterTask.TYPE);
                var storage = CharacterStorage.get();
                if (storage == null) return;
                var handler = context.networkHandler();
                var target = storage.getControllingProfile(handler);
                if (target == null) return;
                var character = storage.allCharacters().get(payload.choice());
                if (character == null) return;
                //TODO: validate character choice
                storage.setCharacter(target.id(), character.id());
            }
        );
    }

    @Environment(EnvType.CLIENT)
    public static void initClient() {
        ClientConfigurationNetworking.registerGlobalReceiver(
            RequestCharacterChoice.TYPE,
            (payload, context) -> context
                .client()
                .setScreen(new BraidScreen(new CharacterPicker(payload.choices, character -> context.responseSender().sendPacket(new PickCharacter(character.id())))))
        );
    }

    public record PickCharacterTask(List<CharacterStorage.Character> choices) implements ConfigurationTask {
        public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(Characteristic.id("pick_character_task").toString());

        @Override
        public void start(@NonNull Consumer<Packet<?>> consumer) {
            consumer.accept(ServerConfigurationNetworking.createS2CPacket(new RequestCharacterChoice(choices)));
        }

        @Override
        public Type type() {
            return TYPE;
        }
    }

    public record RequestCharacterChoice(List<CharacterStorage.Character> choices) implements CustomPacketPayload {
        public static final Type<RequestCharacterChoice> TYPE = new Type<>(Characteristic.id("request_character_choice"));
        public static final Endec<RequestCharacterChoice> ENDEC = StructEndecBuilder.of(
            CharacterStorage.Character.ENDEC
                .listOf()
                .fieldOf("choices", RequestCharacterChoice::choices), RequestCharacterChoice::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record PickCharacter(UUID choice) implements CustomPacketPayload {
        public static final Type<PickCharacter> TYPE = new Type<>(Characteristic.id("pick_character"));
        public static final Endec<PickCharacter> ENDEC = StructEndecBuilder.of(BuiltInEndecs.UUID.fieldOf("choice", PickCharacter::choice), PickCharacter::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static class RequestCharacterSwitcher implements CustomPacketPayload {
        public static final RequestCharacterSwitcher INSTANCE = new RequestCharacterSwitcher();
        public static final Type<RequestCharacterSwitcher> TYPE = new Type<>(Characteristic.id("request_character_switcher"));
        public static final Endec<RequestCharacterSwitcher> ENDEC = Endec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
