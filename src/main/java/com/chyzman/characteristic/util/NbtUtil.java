package com.chyzman.characteristic.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Map;
import java.util.HashMap;

public class NbtUtil {

    public static final Node TEMP_WHITELIST = Node.of(
        "Pos",
        "Rotation",
        "Dimension",
        "Motion",

        "Inventory",
        "equipment", //Fuck you Mojang
        "EnderItems",

        "XpP",
        "XpLevel",
        "XpTotal",
        "XpSeed"
    );

    public static CompoundTag merge(CompoundTag child, CompoundTag parent, Node whitelist) {
        if (whitelist.children.isEmpty()) return child == null ? new CompoundTag() : child;
        if (child == null) child = new CompoundTag();

        for (Map.Entry<String, Node> e : whitelist.children.entrySet()) {
            var key = e.getKey();
            var node = e.getValue();

            var parentTag = parent == null ? null : parent.get(key);
            if (parentTag == null) {
                child.remove(key);
                continue;
            }

            if (node.children.isEmpty() || !(parentTag instanceof CompoundTag parentComp)) {
                child.put(key, parentTag);
                continue;
            }

            child.put(key, merge(child.getCompoundOrEmpty(key), parentComp, node));
        }

        return child;
    }


    public record Node(Map<String, Node> children) {
            public Node(Map<String, Node> children) {
                this.children = children == null ? new HashMap<>() : children;
            }

            public static Node of(String... paths) {
                Map<String, Node> root = new HashMap<>();
                if (paths == null) return new Node(root);

                for (String path : paths) {
                    if (path == null || path.isEmpty()) continue;
                    var current = root;
                    for (String part : path.split("\\.")) {
                        if (part.isEmpty()) continue;
                        var node = current.get(part);
                        if (node == null) {
                            node = new Node(null);
                            current.put(part, node);
                        }
                        current = node.children;
                    }
                }
                return new Node(root);
            }
        }
}
