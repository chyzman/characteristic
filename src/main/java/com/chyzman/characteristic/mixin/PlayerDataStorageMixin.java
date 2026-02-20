package com.chyzman.characteristic.mixin;

import com.chyzman.characteristic.Characteristic;
import com.chyzman.characteristic.cca.CharacterStorage;
import com.chyzman.characteristic.util.NbtUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Mixin(PlayerDataStorage.class)
public abstract class PlayerDataStorageMixin {

    @Shadow
    protected abstract Optional<CompoundTag> load(NameAndId nameAndId, String string);

    @Shadow
    public abstract Optional<CompoundTag> load(NameAndId nameAndId);

    @Shadow @Final private static Logger LOGGER;

    @Inject(
        method = "load(Lnet/minecraft/server/players/NameAndId;Ljava/lang/String;)Ljava/util/Optional;",
        at = @At(value = "RETURN"),
        cancellable = true
    )
    private void mergeSyncedPlayerData$load(NameAndId nameAndId, String string, CallbackInfoReturnable<Optional<CompoundTag>> cir) {
        var storage = CharacterStorage.get();
        if (storage == null) return;
        var character = storage.getCharacterFromCharacter(nameAndId.id());
        if (character == null) return;
        if (character.syncParent == null) return;
        var parent = storage.getCharacterFromCharacter(character.syncParent);
        if (parent == null) return;
        var parentData = load(new NameAndId(parent.profile), string).orElse(null);
        if (parentData == null) return;
        cir.setReturnValue(Optional.of(NbtUtil.merge(cir.getReturnValue().orElse(new CompoundTag()), parentData, NbtUtil.TEMP_WHITELIST)));
    }

    @WrapOperation(
        method = "save",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)V")
    )
    private void mergeSyncedPlayerData$save(
        CompoundTag tag,
        Path playerPath,
        Operation<Void> original,
        @Local(argsOnly = true) Player player,
        @Local(ordinal = 0) Path playerDir
    ) {
        var storage = CharacterStorage.get();
        if (storage != null) {
            var character = storage.getCharacterFromCharacter(player.getUUID());
            if (character != null && character.syncParent != null) {
                var parent = storage.getCharacterFromCharacter(character.syncParent);
                if (parent != null) {
                    CompoundTag parentData;
                    try {
                        parentData = load(new NameAndId(parent.profile)).orElse(null);
                        if (parentData != null) {
                            parentData = NbtUtil.merge(parentData, tag, NbtUtil.TEMP_WHITELIST);
                            Path tempParentFile = Files.createTempFile(playerDir, parent.id() + "-", ".dat");
                            NbtIo.writeCompressed(parentData, tempParentFile);
                            Util.safeReplaceFile(
                                playerDir.resolve(parent.id() + ".dat"),
                                tempParentFile,
                                playerDir.resolve(parent.id() + ".dat_old")
                            );
                        }
                    } catch (Exception e) {
                        Characteristic.LOGGER.warn("Failed to merge synced character data into parent for {}", player.getPlainTextName());
                    }
                    tag = NbtUtil.merge(tag, new CompoundTag(), NbtUtil.TEMP_WHITELIST);
                }

            }
        }
        original.call(tag, playerPath);

    }
}
