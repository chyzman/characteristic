package com.chyzman.characteristic.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.scores.ScoreboardSaveData;
import org.ladysnake.cca.internal.scoreboard.CcaPackedState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ScoreboardSaveData.Packed.class)
public abstract class CCASkillIssueMixin {
    @WrapMethod(method = "equals")
    private boolean fixEquality(Object object, Operation<Boolean> original) {
        if (!(this instanceof CcaPackedState thisPacked) || !(object instanceof CcaPackedState otherPacked)) return original.call(object);
        return original.call(object) && Objects.equals(thisPacked.cca$getSerializedComponents(), otherPacked.cca$getSerializedComponents());
    }
}
