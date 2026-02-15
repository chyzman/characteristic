//package com.chyzman.characteristic.cursed;
//
//import com.chyzman.characteristic.Characteristic;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.multiplayer.ClientPacketListener;
//import net.minecraft.client.multiplayer.CommonListenerCookie;
//import net.minecraft.core.Holder;
//import net.minecraft.core.RegistryAccess;
//import net.minecraft.core.registries.Registries;
//import net.minecraft.data.worldgen.DimensionTypes;
//import net.minecraft.network.Connection;
//import net.minecraft.network.protocol.PacketFlow;
//import net.minecraft.resources.Identifier;
//import net.minecraft.resources.ResourceKey;
//import net.minecraft.world.Difficulty;
//import net.minecraft.world.level.dimension.DimensionType;
//import org.jspecify.annotations.NonNull;
//
//public class CursedClientLevel extends ClientLevel {
//    private static final Identifier DUMMY_DIMMENSION_ID = Characteristic.id("dummy");
//
//    public CursedClientLevel(
//        Holder<DimensionType> holder,
//        RegistryAccess.Frozen registryAccess,
//        CommonListenerCookie commonListenerCookie
//    ) {
//        super(
//            new DummyPacketListener(commonListenerCookie, registryAccess),
//            new ClientLevelData(Difficulty.NORMAL, false, false),
//            ResourceKey.create(Registries.DIMENSION, DUMMY_DIMMENSION_ID),
//            registryAccess.getOrThrow(Registries.DIMENSION_TYPE).value().wrapAsHolder(),
//            0,
//            0,
//            Minecraft.getInstance().levelRenderer,
//            false,
//            0,
//            60
//        );
//    }
//
//    public static class DummyPacketListener extends ClientPacketListener {
//        private final RegistryAccess.Frozen registryAccess;
//
//        public DummyPacketListener(CommonListenerCookie commonListenerCookie, RegistryAccess.Frozen registryAccess) {
//            super(
//                Minecraft.getInstance(),
//                new Connection(PacketFlow.CLIENTBOUND),
//                commonListenerCookie
//            );
//            this.registryAccess = registryAccess;
//        }
//
//        @Override
//        public RegistryAccess.@NonNull Frozen registryAccess() {
//            return registryAccess;
//        }
//    }
//}
