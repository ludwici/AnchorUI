package com.ludwici.anchorui.api.state;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UiNavigationResourcesTest {
    private JsonObject read(String id) throws Exception {
        String path = "assets/anchorui/anchorui/screens/" + id + ".json";
        var input = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(input, "Missing navigation screen " + path);
        try (var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    @Test
    public void navigationExampleTransfersSelectedKeysButNotUnmappedState() throws Exception {
        JsonObject sourceScreen = read("lifecycle_source_demo");
        JsonObject targetScreen = read("lifecycle_target_demo");
        UiLocalState source = new UiLocalState(sourceScreen.getAsJsonObject("state"));
        UiLocalState target = new UiLocalState(targetScreen.getAsJsonObject("state"));

        JsonObject transfer = sourceScreen.getAsJsonArray("entities").get(4).getAsJsonObject()
                .getAsJsonObject("onClick");
        assertEquals("anchorui:open_screen", transfer.get("type").getAsString());
        assertEquals("anchorui:lifecycle_target_demo", transfer.get("screen").getAsString());
        Map<String, String> mapping = UiStateHandoff.parse(transfer, source);
        assertEquals(Map.of("selectedTab", "tab", "showDetails", "details"), mapping);
        UiStateHandoff.copy(source, target, mapping);
        assertEquals("red", target.get("selectedTab").getAsString());
        assertTrue(target.get("showDetails").getAsBoolean());
        assertEquals("kept", target.get("targetOnly").getAsString());

        JsonObject withoutHandoff = sourceScreen.getAsJsonArray("entities").get(5).getAsJsonObject()
                .getAsJsonObject("onClick");
        assertEquals(Map.of(), UiStateHandoff.parse(withoutHandoff, source));
        assertEquals("anchorui:close_screen", targetScreen.getAsJsonArray("entities").get(4).getAsJsonObject().get("onClick").getAsString());
    }

    @Test
    public void screenLifecycleActionsRemainPresentForManualTesting() throws Exception {
        for (String id : new String[]{"lifecycle_source_demo", "lifecycle_target_demo"}) {
            JsonObject screen = read(id);
            assertEquals("anchorui_test:event_notice", screen.get("onOpen").getAsString());
            assertEquals("anchorui_test:event_notice", screen.get("onClose").getAsString());
        }
    }
}
