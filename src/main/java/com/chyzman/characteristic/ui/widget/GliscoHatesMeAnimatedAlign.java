package com.chyzman.characteristic.ui.widget;

import io.wispforest.owo.braid.animation.AlignmentLerp;
import io.wispforest.owo.braid.animation.AutomaticallyAnimatedWidget;
import io.wispforest.owo.braid.animation.DoubleLerp;
import io.wispforest.owo.braid.animation.Easing;
import io.wispforest.owo.braid.core.Alignment;
import io.wispforest.owo.braid.framework.BuildContext;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.widgets.basic.Align;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.OptionalDouble;

public class GliscoHatesMeAnimatedAlign extends AutomaticallyAnimatedWidget {

    public final Alignment alignment;
    public final OptionalDouble widthFactor;
    public final OptionalDouble heightFactor;
    public final Widget child;

    public GliscoHatesMeAnimatedAlign(Duration duration, Easing easing, Alignment alignment, @Nullable Double widthFactor, @Nullable Double heightFactor, Widget child) {
        super(duration, easing);
        this.alignment = alignment;
        this.widthFactor = widthFactor != null ? OptionalDouble.of(widthFactor) : OptionalDouble.empty();
        this.heightFactor = heightFactor != null ? OptionalDouble.of(heightFactor) : OptionalDouble.empty();
        this.child = child;
    }

    public GliscoHatesMeAnimatedAlign(Duration duration, Easing easing, Alignment alignment, Widget child) {
        this(duration, easing, alignment, null, null, child);
    }

    @Override
    public State createState() {
        return new State();
    }

    public static class State extends AutomaticallyAnimatedWidget.State<GliscoHatesMeAnimatedAlign> {

        private AlignmentLerp alignment;
        private DoubleLerp widthFactor;
        private DoubleLerp heightFactor;

        @Override
        protected void updateLerps() {
            this.alignment = this.visitLerp(this.alignment, this.widget().alignment, AlignmentLerp::new);
            if (this.widget().widthFactor.isPresent()) this.widthFactor = this.visitLerp(this.widthFactor, this.widget().widthFactor.orElse(Double.NaN), DoubleLerp::new);
            if (this.widget().heightFactor.isPresent()) this.heightFactor = this.visitLerp(this.heightFactor, this.widget().heightFactor.orElse(Double.NaN), DoubleLerp::new);
        }

        @Override
        public Widget build(BuildContext context) {
            return new Align(
                this.alignment.compute(this.animationValue()),
                this.widthFactor != null ? this.widthFactor.compute(this.animationValue()) : null,
                this.heightFactor != null ? this.heightFactor.compute(this.animationValue()) : null,
                this.widget().child
            );
        }
    }
}
