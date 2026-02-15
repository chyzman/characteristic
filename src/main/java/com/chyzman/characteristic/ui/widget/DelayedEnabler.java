package com.chyzman.characteristic.ui.widget;

import io.wispforest.owo.braid.framework.BuildContext;
import io.wispforest.owo.braid.framework.proxy.WidgetState;
import io.wispforest.owo.braid.framework.widget.StatefulWidget;
import io.wispforest.owo.braid.framework.widget.Widget;
import io.wispforest.owo.braid.widgets.basic.ControlsOverride;

import java.time.Duration;

public class DelayedEnabler extends StatefulWidget {
    final Duration delay;
    final Widget child;

    public DelayedEnabler(Duration delay, Widget child) {
        this.delay = delay;
        this.child = child;
    }

    @Override
    public WidgetState<DelayedEnabler> createState() {
        return new State();
    }

    public static class State extends WidgetState<DelayedEnabler> {
        boolean enabled = false;

        @Override
        public void init() {
            scheduleDelayedCallback(widget().delay, () -> setState(() ->  enabled = true));
        }

        @Override
        public Widget build(BuildContext context) {
            return new ControlsOverride(!enabled, widget().child);
        }
    }
}
