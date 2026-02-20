package com.chyzman.characteristic.mixin;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PropertyMap.class)
public abstract class PropertyMapMixin {

    @Shadow
    @Final
    @Mutable
    private Multimap<String, Property> properties;

    @WrapOperation(
        method = "<init>",
        at = @At(
            value = "FIELD",
            target = "Lcom/mojang/authlib/properties/PropertyMap;properties:Lcom/google/common/collect/Multimap;",
            opcode = Opcodes.PUTFIELD
        )
    )
    private void allowPropertyMutations(
        PropertyMap instance, Multimap<String, Property> value, Operation<Void> original
    ) {
        original.call(instance, HashMultimap.create(value));
    }
}
