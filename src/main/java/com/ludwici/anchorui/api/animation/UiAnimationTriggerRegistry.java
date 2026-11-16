package com.ludwici.anchorui.api.animation;

import com.ludwici.anchorui.api.widget.WidgetContext;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Registers client-side animation trigger logic under a namespaced ID.
 * Sources are sampled only while an animated widget is being rendered.
 */
public final class UiAnimationTriggerRegistry {
    @FunctionalInterface
    public interface RevisionSource {
        long revision(WidgetContext context);
    }

    private static final Map<ResourceLocation, RevisionSource> SOURCES = new ConcurrentHashMap<>();

    private UiAnimationTriggerRegistry() {
    }

    public static void register(ResourceLocation id, RevisionSource source) {
        if (id == null || source == null) {
            throw new IllegalArgumentException("Animation trigger ID and source cannot be null");
        }
        if (SOURCES.putIfAbsent(id, source) != null) {
            throw new IllegalArgumentException("Animation trigger already registered: " + id);
        }
    }

    public static void registerCondition(ResourceLocation id, Predicate<WidgetContext> condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Animation trigger condition cannot be null");
        }
        register(id, context -> condition.test(context) ? 1L : 0L);
    }

    public static OptionalLong sample(ResourceLocation id, WidgetContext context) {
        RevisionSource source = SOURCES.get(id);
        return source == null ? OptionalLong.empty() : OptionalLong.of(source.revision(context));
    }

    public static boolean contains(ResourceLocation id) {
        return SOURCES.containsKey(id);
    }
}
