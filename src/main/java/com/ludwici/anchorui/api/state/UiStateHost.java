package com.ludwici.anchorui.api.state;

/**
 * Implemented by AnchorUI screens that expose ephemeral local UI state.
 * Java actions and data bindings may use this API without referencing
 * the concrete client screen implementation.
 */
public interface UiStateHost {
    UiLocalState uiState();
}
