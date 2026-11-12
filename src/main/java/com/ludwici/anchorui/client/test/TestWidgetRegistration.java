package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.api.action.UiActionRegistry;
import com.ludwici.anchorui.api.binding.DataValueRegistry;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.ludwici.anchorui.api.widget.AnchorWidgetRegistry;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestWidgetRegistration {
    public static final ResourceLocation COUNTER = ResourceLocation.fromNamespaceAndPath("anchorui_test", "counter");
    public static final ResourceLocation BROKEN = ResourceLocation.fromNamespaceAndPath("anchorui_test", "broken");
    public static final ResourceLocation EVENT_NOTICE = ResourceLocation.fromNamespaceAndPath("anchorui_test", "event_notice");
    public static final ResourceLocation QUESTS = ResourceLocation.fromNamespaceAndPath("anchorui_test", "quests");
    public static final ResourceLocation ACCEPT_QUEST = ResourceLocation.fromNamespaceAndPath("anchorui_test", "accept_quest");
    public static final ResourceLocation TOGGLE_QUESTS = ResourceLocation.fromNamespaceAndPath("anchorui_test", "toggle_quests");
    private static DataValueRegistry.MutableValue questItems;
    private static boolean alternateQuestItems;

    private TestWidgetRegistration() {
    }

    public static void register() {
        questItems = DataValueRegistry.registerMutable(QUESTS, JsonParser.parseString("""
                [
                  {
                    "id": "anchorui_test:find_sword",
                    "title": "Find lost sword",
                    "level": 1
                  },
                  {
                    "id": "anchorui_test:kill_dragon",
                    "title": "Defeat dragon",
                    "level": 10
                  }
                ]
                """));
        UiActionRegistry.registerEvent(ACCEPT_QUEST, Codec.STRING.fieldOf("quest").codec(), (context, quest) -> event -> {
            if (context.minecraft().player != null) {
                String selected = event.item().isJsonObject()
                        ? event.item().getAsJsonObject().get("title").getAsString() : quest;
                context.minecraft().player.displayClientMessage(
                        Component.literal("Template List selected " + selected + " [" + quest + "]"), false);
            }
        });
        UiActionRegistry.registerEvent(TOGGLE_QUESTS, (context, settings) -> event -> {
            alternateQuestItems = !alternateQuestItems;
            questItems.set(JsonParser.parseString(alternateQuestItems ? """
                    [
                      {
                        "id": "anchorui_test:kill_dragon",
                        "title": "Defeat dragon",
                        "level": 10
                      },
                      {
                        "id": "anchorui_test:find_sword",
                        "title": "Find lost sword",
                        "level": 1
                      },
                      {
                        "id": "anchorui_test:collect_herbs",
                        "title": "Collect healing herbs",
                        "level": 3
                      },
                      {
                        "id": "anchorui_test:repair_bridge",
                        "title": "Repair the old bridge",
                        "level": 2
                      },
                      {
                        "id": "anchorui_test:visit_ruins",
                        "title": "Explore forgotten ruins",
                        "level": 4
                      },
                      {
                        "id": "anchorui_test:find_cart",
                        "title": "Find a merchant cart",
                        "level": 1
                      }
                    ]
                    """ : """
                    [
                      {
                        "id": "anchorui_test:find_sword",
                        "title": "Find lost sword",
                        "level": 1
                      },
                      {
                        "id": "anchorui_test:kill_dragon",
                        "title": "Defeat dragon",
                        "level": 10
                      }
                    ]
                    """));
        });
        AnchorWidgetRegistry.register(COUNTER, CounterConfig.CODEC, (context, element, style, config) -> {
            AtomicInteger clicks = new AtomicInteger(config.initial());
            return Button.builder(Component.literal("Counter: " + config.initial()), button -> button.setMessage(Component.literal("Counter: " + clicks.addAndGet(config.step())))).bounds(0, 0, 120, 20).build();
        });
        AnchorWidgetRegistry.register(BROKEN, (context, element, style) -> {
            throw new IllegalArgumentException("Intentional error from scene12 factory");
        });
        UiActionRegistry.registerEvent(EVENT_NOTICE, (context, settings) -> event -> {
            if (context.minecraft().player == null) {
                return;
            }
            String value = event.value().isJsonNull() ? "" : " = " + event.value();
            context.minecraft().player.displayClientMessage(Component.literal(
                    "Anchor UI " + event.name() + " [" + event.targetId() + "]" + value), false);
        });
    }
}
