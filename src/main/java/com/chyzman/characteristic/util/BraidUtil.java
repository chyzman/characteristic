package com.chyzman.characteristic.util;

import io.wispforest.owo.braid.widgets.intents.ShortcutTrigger;
import io.wispforest.owo.braid.widgets.intents.Trigger;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class BraidUtil {

    public static final ShortcutTrigger NUM_0 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_0), Trigger.ofKey(GLFW_KEY_KP_0));
    public static final ShortcutTrigger NUM_1 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_1), Trigger.ofKey(GLFW_KEY_KP_1));
    public static final ShortcutTrigger NUM_2 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_2), Trigger.ofKey(GLFW_KEY_KP_2));
    public static final ShortcutTrigger NUM_3 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_3), Trigger.ofKey(GLFW_KEY_KP_3));
    public static final ShortcutTrigger NUM_4 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_4), Trigger.ofKey(GLFW_KEY_KP_4));
    public static final ShortcutTrigger NUM_5 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_5), Trigger.ofKey(GLFW_KEY_KP_5));
    public static final ShortcutTrigger NUM_6 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_6), Trigger.ofKey(GLFW_KEY_KP_6));
    public static final ShortcutTrigger NUM_7 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_7), Trigger.ofKey(GLFW_KEY_KP_7));
    public static final ShortcutTrigger NUM_8 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_8), Trigger.ofKey(GLFW_KEY_KP_8));
    public static final ShortcutTrigger NUM_9 = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_9), Trigger.ofKey(GLFW_KEY_KP_9));

    public static final List<ShortcutTrigger> NUMBERS = List.of(NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9, NUM_0);

    public static final ShortcutTrigger ENTER = new ShortcutTrigger(Trigger.ofKey(GLFW_KEY_ENTER), Trigger.ofKey(GLFW_KEY_KP_ENTER));
}
