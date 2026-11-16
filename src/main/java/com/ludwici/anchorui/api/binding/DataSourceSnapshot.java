package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.ludwici.anchorui.api.widget.WidgetContext;

import java.util.Objects;

/**
 * Per-binding snapshot for opt-in revisioned data providers. Ordinary live
 * providers are always evaluated; each binding owns its own cached snapshot.
 */
public final class DataSourceSnapshot {
    private record Cached(WidgetContext context, long revision, JsonElement value) {
    }

    private final DataValueRegistry.Provider provider;
    private Cached cached;

    public DataSourceSnapshot(DataValueRegistry.Provider provider) {
        this.provider = Objects.requireNonNull(provider, "Data provider cannot be null");
    }

    public JsonElement read(WidgetContext context) {
        if (!(provider instanceof DataValueRegistry.RevisionedProvider revisioned)) {
            return provider.read(context);
        }

        long revision = revisioned.revision(context);
        Cached current = cached;
        if (current != null && current.context() == context && current.revision() == revision) {
            return current.value().deepCopy();
        }

        JsonElement value = provider.read(context);
        JsonElement snapshot = value == null ? JsonNull.INSTANCE : value.deepCopy();
        if (revisioned.revision(context) == revision) {
            cached = new Cached(context, revision, snapshot);
        } else {
            cached = null;
        }
        return snapshot.deepCopy();
    }
}
