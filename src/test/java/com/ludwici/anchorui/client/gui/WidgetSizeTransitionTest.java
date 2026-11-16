package com.ludwici.anchorui.client.gui;

import com.google.gson.JsonParser;
import com.ludwici.anchorui.api.animation.UiAnimationSpec;
import com.ludwici.anchorui.api.layout.AnchorLayout;
import com.ludwici.anchorui.api.layout.WidgetTransforms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WidgetSizeTransitionTest {
    private static WidgetSizeTransition parse(String json, boolean progress) {
        return WidgetSizeTransition.parse(JsonParser.parseString(json).getAsJsonObject(), null, progress);
    }

    @Test
    public void genericImageOrPanelWidthIsCroppedFromItsOriginalBounds() {
        WidgetSizeTransition transition = parse("""
                {
                  "sizeTransition": {
                    "duration": 200,
                    "easing": "linear",
                    "width": {
                      "value": 25,
                      "max": 100
                    },
                    "height": {
                      "value": 2,
                      "max": 4
                    }
                  }
                }
                """, false);
        assertNotNull(transition);
        AnchorLayout.Bounds bounds = transition.clippedBounds(new AnchorLayout.Bounds(12, 8, 200, 20), WidgetTransforms.IDENTITY, UiAnimationSpec.Frame.IDENTITY, 0L);
        assertEquals(12, bounds.x());
        assertEquals(8, bounds.y());
        assertEquals(50, bounds.width());
        assertEquals(10, bounds.height());
    }

    @Test
    public void progressUsesSameWidthInterpolatorWithImplicitFillFraction() {
        WidgetSizeTransition transition = parse("""
                {"sizeTransition":{"duration":200,"easing":"linear"}}
                """, true);
        assertEquals(1.0, transition.widthFraction(1, 0L), 0.00001);
        assertEquals(1.0, transition.widthFraction(0.2, 100000000L), 0.00001);
        assertEquals(0.6, transition.widthFraction(0.2, 200000000L), 0.00001);
        assertEquals(0.2, transition.widthFraction(0.2, 300000000L), 0.00001);
    }

    @Test
    public void invalidSettingsAreRejectedAndAbsentSettingsAreNoop() {
        assertNull(parse("{}", false));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"sizeTransition\":{\"duration\":0}}", true));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"sizeTransition\":{}}", false));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"sizeTransition\":{\"width\":{\"value\":1}}}", false));
        assertThrows(IllegalArgumentException.class, () -> parse("{\"sizeTransition\":{\"duration\":500,\"easing\":\"bounce\"}}", true));
    }
}
