package com.chyzman.characteristic.mixin.client;

import com.chyzman.characteristic.network.CharacterHandler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Environment(EnvType.CLIENT)
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {

    @Shadow @Final private static int BUTTON_WIDTH_FULL;

    @Unique private static final Button.Builder CHARACTERISTIC_BUTTON =
        Button.builder(
                Component.translatable("menu.characteristic.changeCharacter"),
                button -> {
                    button.active = false;
                    ClientPlayNetworking.send(CharacterHandler.C2SOpenCharacterSwitcher.INSTANCE);
                }
            )
            .width(BUTTON_WIDTH_FULL);


//    @Inject(method = "createPauseMenu",
//        at = @At(
//            value = "INVOKE",
//            target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
//            ordinal = 2
//        )
//    )
//    private void addCharacteristicButton(CallbackInfo ci, @Local GridLayout.RowHelper rowHelper) {
//        rowHelper.addChild(
//            Button.builder(
//                    Component.translatable("menu.characteristic.changeCharacter"),
//                    button -> {
//                        button.active = false;
//                        Minecraft.getInstance().player.displayClientMessage(Component.literal("Amogus"), false);
//                    }
//                )
//                .width(204)
//                .build(),
//            2
//        );
//    }

    @WrapMethod(method = "addFeedbackButtons")
    private static void replaceNormalFeedbackLine(Screen screen, GridLayout.RowHelper rowHelper, Operation<Void> original) {
        rowHelper.addChild(CHARACTERISTIC_BUTTON.build(), 2);
    }

    @WrapOperation(
        method = "addFeedbackSubscreenAndCustomDialogButtons",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
            ordinal = 0
        )
    )
    private static LayoutElement addCharacteristicButton(GridLayout.RowHelper instance, LayoutElement layoutElement, Operation<LayoutElement> original) {
        return original.call(instance, CHARACTERISTIC_BUTTON.build());
    }
}
