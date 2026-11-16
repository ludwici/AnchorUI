package com.ludwici.anchorui.api.binding;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataFormatterRegistryTest {
    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("formatter_test", path);
    }

    private static JsonObject json(String value) {
        return JsonParser.parseString(value).getAsJsonObject();
    }

    @Test
    public void formatsNumericSourceToTextAfterResolvingBindingPath() {
        DataValueRegistry.registerObject(id("stats"), context -> json("""
                {
                  "stats": {
                    "mana": 125
                  }
                }
                """));
        DataFormatterRegistry.register(id("mana"), (value, context) -> new JsonPrimitive(value.getAsInt() + " MP"));
        BoundData binding = BoundData.parse(json("""
                {
                  "text": {
                    "bind": "formatter_test:stats",
                    "path": "stats.mana",
                    "format": "formatter_test:mana",
                    "fallback": "No mana"
                  }
                }
                """), "text", new JsonPrimitive(""), DataBindingSpec.Kind.STRING);
        assertEquals("125 MP", binding.get(null).getAsString());
    }

    @Test
    public void compilesConfiguredFormatterOnceAndUsesCodecValidation() {
        DataValueRegistry.registerNumber(id("score"), context -> 42);
        DataFormatterRegistry.register(id("suffix"), Codec.STRING.fieldOf("suffix").codec(), (value, context, suffix) -> new JsonPrimitive(value.getAsInt() + suffix));

        BoundData binding = BoundData.parse(json("""
                {
                  "text": {
                    "bind": "formatter_test:score",
                    "format": {
                      "type": "formatter_test:suffix",
                      "suffix": " XP"
                    },
                    "fallback": "Missing"
                  }
                }
                """), "text", new JsonPrimitive(""), DataBindingSpec.Kind.STRING);
        assertEquals("42 XP", binding.get(null).getAsString());
        assertEquals("42 XP", binding.get(null).getAsString());

        assertThrows(IllegalArgumentException.class, () -> BoundData.parse(json("""
                {
                  "text": {
                    "bind": "formatter_test:score",
                    "format": {
                      "type": "formatter_test:suffix"
                    },
                    "fallback": "Missing"
                  }
                }
                """), "text", new JsonPrimitive(""), DataBindingSpec.Kind.STRING));
    }

    @Test
    public void typedFormatterCodecReceivesOnlyConfigurationFields() {
        DataValueRegistry.registerString(id("metadata_input"), context -> "anything");
        DataFormatterRegistry.register(id("metadata_free"), Codec.PASSTHROUGH, (value, context, options) -> {
            JsonObject arguments = options.convert(JsonOps.INSTANCE).getValue().getAsJsonObject();
            return new JsonPrimitive(arguments.has("type") ? "type leaked to codec" : arguments.get("suffix").getAsString());
        });
        BoundData binding = BoundData.parse(json("""
                {
                  "text": {
                    "bind": "formatter_test:metadata_input",
                    "format": {
                      "type": "formatter_test:metadata_free",
                      "suffix": "clean"
                    },
                    "fallback": "missing"
                  }
                }
                """), "text", new JsonPrimitive(""), DataBindingSpec.Kind.STRING);
        assertEquals("clean", binding.get(null).getAsString());
    }

    @Test
    public void formattersWorkInEscapedTextTemplates() {
        DataValueRegistry.registerNumber(id("level"), context -> 25);
        DataFormatterRegistry.register(id("text"), (value, context) -> new JsonPrimitive("<" + value.getAsInt() + ">"));
        BoundText binding = BoundText.parse(new JsonPrimitive("Level: {{formatter_test:level|formatter_test:text}}"));
        assertTrue(binding.isDynamic());
        assertEquals("Level: ‹25›", binding.get(null));
    }

    @Test
    public void malformedOutputsAndRuntimeErrorsReturnFallback() {
        DataValueRegistry.registerNumber(id("power"), context -> 10);
        DataValueRegistry.register(id("missing"), context -> JsonNull.INSTANCE);
        DataFormatterRegistry.register(id("wrong_type"), (value, context) -> new JsonPrimitive(999));
        DataFormatterRegistry.register(id("exception"), (value, context) -> {
            throw new IllegalStateException("Demo error");
        });
        DataFormatterRegistry.register(id("null_output"), (value, context) -> null);

        for (String formatter : new String[]{"formatter_test:wrong_type", "formatter_test:exception", "formatter_test:null_output"}) {
            JsonObject source = new JsonObject();
            JsonObject setting = new JsonObject();
            setting.addProperty("bind", "formatter_test:power");
            setting.addProperty("format", formatter);
            setting.addProperty("fallback", "Unavailable");
            source.add("text", setting);
            assertEquals("Unavailable", BoundData.parse(source, "text", new JsonPrimitive(""), DataBindingSpec.Kind.STRING).get(null).getAsString());
        }

        BoundData missing = BoundData.parse(json("""
                {
                  "text": {
                    "bind": "formatter_test:missing",
                    "format": "formatter_test:exception",
                    "fallback": "Missing"
                  }
                }
                """), "text", new JsonPrimitive(""), DataBindingSpec.Kind.STRING);
        assertEquals("Missing", missing.get(null).getAsString());
    }

    @Test
    public void validatesIdsUnknownFormatsAndDuplicateRegistration() {
        assertNull(DataFormatterRegistry.compile(null));
        assertNull(DataFormatterRegistry.compile(JsonNull.INSTANCE));
        assertThrows(IllegalArgumentException.class, () -> DataFormatterRegistry.compile(new JsonPrimitive("no_namespace")));
        assertThrows(IllegalArgumentException.class, () -> DataFormatterRegistry.compile(new JsonPrimitive("formatter_test:unknown")));
        assertThrows(IllegalArgumentException.class, () -> DataFormatterRegistry.compile(new JsonPrimitive(4)));
        assertThrows(IllegalArgumentException.class, () -> DataFormatterRegistry.compile(json("""
                {
                  "precision": 2
                }
                """)));

        DataFormatterRegistry.register(id("duplicate"), (value, context) -> value);
        assertNotNull(DataFormatterRegistry.compile(new JsonPrimitive("formatter_test:duplicate")));
        assertThrows(IllegalStateException.class, () -> DataFormatterRegistry.register(id("duplicate"), (value, context) -> value));
    }

    @Test
    public void builtinsTransformNumbersAndStrings() {
        BuiltinDataFormatters.register();
        assertEquals("26%", DataFormatterRegistry.compile(new JsonPrimitive("anchorui:percent")).format(new JsonPrimitive(0.256), null).getAsString());
        assertEquals("12.5K", DataFormatterRegistry.compile(new JsonPrimitive("anchorui:compact")).format(new JsonPrimitive(12500), null).getAsString());
        assertEquals("12", DataFormatterRegistry.compile(new JsonPrimitive("anchorui:integer")).format(new JsonPrimitive(11.6), null).getAsString());
        assertEquals("HELLO", DataFormatterRegistry.compile(new JsonPrimitive("anchorui:uppercase")).format(new JsonPrimitive("Hello"), null).getAsString());
        assertThrows(IllegalArgumentException.class, () -> DataFormatterRegistry.compile(new JsonPrimitive("anchorui:compact")).format(new JsonPrimitive("hello"), null));
    }
}
