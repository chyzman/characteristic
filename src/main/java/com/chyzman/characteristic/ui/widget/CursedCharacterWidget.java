package com.chyzman.characteristic.ui.widget;

import com.chyzman.characteristic.cca.CharacterStorage;
import com.mojang.math.Axis;
import io.wispforest.owo.braid.core.BraidGraphics;
import io.wispforest.owo.braid.core.Constraints;
import io.wispforest.owo.braid.core.element.BraidEntityElement;
import io.wispforest.owo.braid.framework.instance.LeafWidgetInstance;
import io.wispforest.owo.braid.framework.widget.LeafInstanceWidget;
import io.wispforest.owo.braid.framework.widget.WidgetSetupCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.function.Consumer;

public class CursedCharacterWidget extends LeafInstanceWidget {

    public final double scale;
    public final CharacterStorage.Character character;
    public final AvatarRenderState renderState = new AvatarRenderState();

    protected DisplayMode displayMode = DisplayMode.FIXED;
    protected boolean scaleToFit = true;
    protected boolean showNametag = false;
    protected @Nullable Consumer<Matrix4f> transform = null;

    public CursedCharacterWidget(double scale, CharacterStorage.Character character, @Nullable WidgetSetupCallback<CursedCharacterWidget> setupCallback) {
        this.scale = scale;
        this.character = character;
        renderState.boundingBoxWidth = Avatar.DEFAULT_BB_WIDTH;
        renderState.boundingBoxHeight = Avatar.DEFAULT_BB_HEIGHT;
        renderState.eyeHeight = Avatar.DEFAULT_EYE_HEIGHT;
        var client = Minecraft.getInstance();
        var options = client.options;
        renderState.showHat = options.isModelPartEnabled(PlayerModelPart.HAT);
        renderState.showJacket = options.isModelPartEnabled(PlayerModelPart.JACKET);
        renderState.showLeftSleeve = options.isModelPartEnabled(PlayerModelPart.LEFT_SLEEVE);
        renderState.showRightSleeve = options.isModelPartEnabled(PlayerModelPart.RIGHT_SLEEVE);
        renderState.showLeftPants = options.isModelPartEnabled(PlayerModelPart.LEFT_PANTS_LEG);
        renderState.showRightPants = options.isModelPartEnabled(PlayerModelPart.RIGHT_PANTS_LEG);
        renderState.showCape = options.isModelPartEnabled(PlayerModelPart.CAPE);
        renderState.mainArm = options.mainHand().get();
        renderState.showExtraEars = "deadmau5".equals(character.name());
        renderState.isUpsideDown = "Dinnerbone".equals(character.name()) || "Grumm".equals(character.name());
        Minecraft.getInstance().getSkinManager().get(character.profile()).thenAccept(playerSkin -> playerSkin.ifPresent((skin) -> renderState.skin = skin));

        if (setupCallback != null) setupCallback.setup(this);
    }

    public CursedCharacterWidget displayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
        return this;
    }

    public DisplayMode displayMode() {
        return this.displayMode;
    }

    public CursedCharacterWidget scaleToFit(boolean scaleToFit) {
        this.scaleToFit = scaleToFit;
        return this;
    }

    public boolean scaleToFit() {
        return this.scaleToFit;
    }

    public CursedCharacterWidget showNametag(boolean showNametag) {
        this.showNametag = showNametag;
        return this;
    }

    public boolean showNametag() {
        return this.showNametag;
    }

    public CursedCharacterWidget transform(Consumer<Matrix4f> transform) {
        this.transform = transform;
        return this;
    }

    public @Nullable Consumer<Matrix4f> transform() {
        return this.transform;
    }

    @Override
    public LeafWidgetInstance<?> instantiate() {
        return new Instance(this);
    }

    public static class Instance extends LeafWidgetInstance<CursedCharacterWidget> {

        protected double baseScale = 1.0;

        public Instance(CursedCharacterWidget widget) {
            super(widget);
        }

        @Override
        public void setWidget(CursedCharacterWidget widget) {
            if (this.widget.scaleToFit != widget.scaleToFit) {
                this.markNeedsLayout();
            }

            super.setWidget(widget);
        }

        @Override
        protected void doLayout(Constraints constraints) {
            this.transform.setSize(constraints.minSize());

            if (this.widget.scaleToFit) {
                this.baseScale = Math.min(
                    this.transform.width() / this.widget.renderState.boundingBoxWidth,
                    this.transform.height() / this.widget.renderState.boundingBoxHeight
                ) * .6;
            }
        }

        @Override
        protected double measureIntrinsicWidth(double height) {
            return 32;
        }

        @Override
        protected double measureIntrinsicHeight(double width) {
            return 32;
        }

        @Override
        protected OptionalDouble measureBaselineOffset() {
            return OptionalDouble.empty();
        }

        @Override
        public void draw(BraidGraphics graphics) {
            var state = this.widget.renderState;

            var entitySpaceToWidgetSpace = new Matrix4f();
            entitySpaceToWidgetSpace.translate(0, (float) (this.transform.height() / 2), 100);
            entitySpaceToWidgetSpace.scale((float) (this.widget.scale * this.baseScale));
            entitySpaceToWidgetSpace.scale(1, -1, -1);

            var entityTransform = new Matrix4f();
            if (this.widget.transform != null) {
                this.widget.transform.accept(entityTransform);
            }

            entityTransform.translate(0, -state.boundingBoxHeight / 2, 0);

            var xRotation = 0f;
            var yRotation = 0f;

//            var lastHeadYaw = state.yRot;
//            var lastYaw = state.bodyRot;
//            var lastPitch = state.xRot;

            if (this.widget.displayMode == DisplayMode.FIXED) {
                xRotation = 35;
                yRotation = -45;
            } else if (this.widget.displayMode != DisplayMode.NONE) {
                var globalCursorPos = this.host().cursorPosition();
                var cursor4x4Buffer = graphics.pose().get4x4(new float[16]);

                var cursorTransform = new Matrix4f()
                    .set(cursor4x4Buffer)
                    // we do this ugly cursor-specific offset here to account for the
                    // centering being indiscriminately applied inside the PIP renderer
                    .translate((float) (this.transform.width() / 2), 0, 0)
                    .mul(entitySpaceToWidgetSpace)
                    .mul(entityTransform)
                    .invert();

                var localCursorPos = cursorTransform.transform(new Vector4f((float) globalCursorPos.x(), (float) globalCursorPos.y(), 0, 1));

                switch (widget.displayMode) {
                    case CURSOR -> {
                        var center = new Vector4f(0, state.eyeHeight, 0, 1);

                        xRotation = (float) Math.toDegrees(Math.atan(localCursorPos.y - center.y)) * -.15f;
                        yRotation = (float) Math.toDegrees(Math.atan(localCursorPos.x - center.x)) * .15f;
                        state.yRot = -yRotation * 3;

                        state.bodyRot = -yRotation * .65f;
                        state.xRot = xRotation * 2.5f;
                    }
                    case VANILLA -> {
                        var center = new Vector4f(0, state.boundingBoxHeight / 2, 0, 1);

                        xRotation = (float) Math.atan(localCursorPos.y - center.y) * -20f;
                        yRotation = (float) Math.atan(localCursorPos.x - center.x) * 20f;
                        state.yRot = -yRotation;

                        state.bodyRot = -yRotation;
                        state.xRot = xRotation;
                    }
                }
            }

            // We make sure the yRotation never becomes 0, as the lighting otherwise becomes very unhappy
            if (yRotation == 0) yRotation = .1f;

            entityTransform.rotate(Axis.XP.rotationDegrees(xRotation));
            entityTransform.rotate(Axis.YP.rotationDegrees(yRotation));

//            var entityState = this.host().client().getEntityRenderDispatcher().extractEntity(this.widget.renderState, 0);

            if (!this.widget.showNametag) {
                state.nameTag = null;
            }

            graphics.guiRenderState.submitPicturesInPictureState(new BraidEntityElement(
                state,
                new Matrix4f().mul(entitySpaceToWidgetSpace).mul(entityTransform),
                new Matrix3x2f(graphics.pose()),
                this.transform.width(), this.transform.height(),
                graphics.scissorStack.peek()
            ));

//            state.yRot = lastHeadYaw;
//            state.xRot = lastPitch;
//            state.bodyRot = lastYaw;
        }
    }

    public enum DisplayMode {
        FIXED, VANILLA, CURSOR, NONE
    }
}
