package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.KeyRegistry;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.animation.UiAnimationTriggerRegistry;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.ScreenIds;
import com.ludwici.anchorui.client.UserDocumentStore;
import com.ludwici.anchorui.client.hud.AnchorHudManager;
import com.ludwici.anchorui.client.hud.AnchorHudEditorScreen;
import com.ludwici.anchorui.client.gui.ItemLoreScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import java.util.concurrent.atomic.AtomicLong;
import java.io.IOException;

@EventBusSubscriber(modid = AnchorUI.MODID, value = Dist.CLIENT)
public final class AnchorUITestCommands {
    private static final AtomicLong TEST_PULSE_REVISION = new AtomicLong();

    static {
        UiAnimationTriggerRegistry.register(ResourceLocation.fromNamespaceAndPath("anchorui", "test_pulse"), context -> TEST_PULSE_REVISION.get());
    }

    private static TestScene pending;
    private static boolean pendingHudEditor;
    private static ResourceLocation pendingScreen;

    private AnchorUITestCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        var test = Commands.literal("test")
                .executes(context -> listScenes())
                .then(Commands.literal("list").executes(context -> listScenes()));

        registerHudTest(test, "hud1");
        registerHudTest(test, "rpg");
        registerHudTest(test, "animation");
        test.then(Commands.literal("pulse").executes(context -> testPulse()));
        registerHudTest(test, "binding");
        registerHudTest(test, "conditions");
        registerHudTest(test, "vanilla");

        for (TestScene scene : TestScene.values()) {
            test.then(Commands.literal(scene.id()).executes(context -> queueScene(scene)));
        }

        var hud = Commands.literal("hud");
        var hudEdit = Commands.literal("edit").executes(context -> queueHudEditor());
        hudEdit.then(Commands.argument("id", StringArgumentType.greedyString()).suggests((context, builder) -> suggestLocal(UserDocumentStore.Kind.HUD, builder)).executes(context -> queueHudEditor(StringArgumentType.getString(context, "id"))));
        hud.then(hudEdit);
        var hudCreate = Commands.literal("create");
        hudCreate.then(Commands.argument("id", StringArgumentType.greedyString()).executes(context -> createDocument(UserDocumentStore.Kind.HUD, StringArgumentType.getString(context, "id"))));
        hud.then(hudCreate);
        var hudDelete = Commands.literal("delete");
        hudDelete.then(Commands.argument("id", StringArgumentType.greedyString()).suggests((context, builder) -> suggestLocal(UserDocumentStore.Kind.HUD, builder)).executes(context -> deleteDocument(UserDocumentStore.Kind.HUD, StringArgumentType.getString(context, "id"))));
        hud.then(hudDelete);
        hud.then(Commands.literal("list").executes(context -> listDocuments(UserDocumentStore.Kind.HUD)));

        var screens = Commands.literal("screen");
        var openScreen = Commands.literal("open");
        openScreen.then(Commands.argument("id", StringArgumentType.greedyString()).suggests((context, builder) -> {
            LoreLoader.INSTANCE.getData().keySet().stream().filter(id -> id.getPath().startsWith("screens/")).map(ScreenIds::display).sorted().forEach(builder::suggest);
            return builder.buildFuture();
        }).executes(context -> queueScreen(StringArgumentType.getString(context, "id"))));
        screens.then(openScreen);
        var screenCreate = Commands.literal("create");
        screenCreate.then(Commands.argument("id", StringArgumentType.greedyString()).executes(context -> createDocument(UserDocumentStore.Kind.SCREEN, StringArgumentType.getString(context, "id"))));
        screens.then(screenCreate);
        var screenDelete = Commands.literal("delete");
        screenDelete.then(Commands.argument("id", StringArgumentType.greedyString()).suggests((context, builder) -> suggestLocal(UserDocumentStore.Kind.SCREEN, builder)).executes(context -> deleteDocument(UserDocumentStore.Kind.SCREEN, StringArgumentType.getString(context, "id"))));
        screens.then(screenDelete);
        screens.then(Commands.literal("list").executes(context -> listDocuments(UserDocumentStore.Kind.SCREEN)));
        event.getDispatcher().register(Commands.literal("anchorui").then(test).then(hud).then(screens));
    }

    private static void registerHudTest(com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> test, String group) {
        test.then(Commands.literal(group).executes(context -> setHud(group, !AnchorHudManager.INSTANCE.isTestEnabled(group))).then(Commands.literal("on").executes(context -> setHud(group, true))).then(Commands.literal("off").executes(context -> setHud(group, false))));
    }

    private static int testPulse() {
        TEST_PULSE_REVISION.incrementAndGet();
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal("Anchor UI animation trigger: anchorui:test_pulse"), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int setHud(String group, boolean enabled) {
        AnchorHudManager.INSTANCE.setTestEnabled(group, enabled);
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal("Anchor UI HUD test '" + group + "': " + (enabled ? "enabled" : "disabled")), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int queueScreen(String rawId) {
        ResourceLocation id = ScreenIds.resolve(rawId);
        if (id == null) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(Component.literal("Anchor UI: screen id must be namespace:name"), false);
            }
            return 0;
        }
        pendingScreen = id;
        return Command.SINGLE_SUCCESS;
    }

    private static void openPendingScreen(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        LoreEntity entity = LoreLoader.INSTANCE.getData().get(id);
        if (entity == null) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("Anchor UI: screen '" + id + "' not found in client resources"), false);
            }
            return;
        }
        minecraft.setScreen(new ItemLoreScreen(minecraft.screen, ItemStack.EMPTY, entity, id));
    }

    private static int queueHudEditor() {
        AnchorHudManager.INSTANCE.setActiveUserHud(null);
        pendingHudEditor = true;
        return Command.SINGLE_SUCCESS;
    }

    private static int queueHudEditor(String rawId) {
        ResourceLocation id = UserDocumentStore.resolve(UserDocumentStore.Kind.HUD, rawId);
        if (id == null || !UserDocumentStore.exists(UserDocumentStore.Kind.HUD, id)) {
            notifyPlayer("Anchor UI: HUD '" + rawId + "' is not a locally created HUD");
            return 0;
        }
        AnchorHudManager.INSTANCE.setActiveUserHud(id);
        pendingHudEditor = true;
        return Command.SINGLE_SUCCESS;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestLocal(
            UserDocumentStore.Kind kind, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (ResourceLocation id : UserDocumentStore.list(kind)) {
            builder.suggest(UserDocumentStore.display(kind, id));
        }
        return builder.buildFuture();
    }

    private static int createDocument(UserDocumentStore.Kind kind, String rawId) {
        ResourceLocation id = UserDocumentStore.resolve(kind, rawId);
        if (id == null) {
            notifyPlayer("Anchor UI: ID must be namespace:name");
            return 0;
        }
        boolean registered = kind == UserDocumentStore.Kind.HUD
                ? LoreLoader.INSTANCE.getHudDocuments().containsKey(id)
                : LoreLoader.INSTANCE.getData().containsKey(id);
        if (registered || UserDocumentStore.exists(kind, id)) {
            notifyPlayer("Anchor UI: document '" + UserDocumentStore.display(kind, id) + "' already exists");
            return 0;
        }
        try {
            UserDocumentStore.create(kind, id);
            LoreLoader.INSTANCE.refreshUserDocuments();
            registered = kind == UserDocumentStore.Kind.HUD
                    ? LoreLoader.INSTANCE.getHudDocuments().containsKey(id)
                    : LoreLoader.INSTANCE.getData().containsKey(id);
            if (!registered) {
                UserDocumentStore.delete(kind, id);
                LoreLoader.INSTANCE.refreshUserDocuments();
                notifyPlayer("Anchor UI: new document could not be loaded");
                return 0;
            }
            if (kind == UserDocumentStore.Kind.HUD) {
                AnchorHudManager.INSTANCE.setActiveUserHud(id);
                pendingHudEditor = true;
            } else {
                pendingScreen = id;
            }
            notifyPlayer("Anchor UI: created " + kind.name().toLowerCase(java.util.Locale.ROOT) + " '" + UserDocumentStore.display(kind, id) + "'");
            return Command.SINGLE_SUCCESS;
        } catch (IOException | RuntimeException exception) {
            AnchorUI.LOGGER.error("Failed to create Anchor UI document {}", id, exception);
            notifyPlayer("Anchor UI: create failed: " + exception.getMessage());
            return 0;
        }
    }

    private static int deleteDocument(UserDocumentStore.Kind kind, String rawId) {
        ResourceLocation id = UserDocumentStore.resolve(kind, rawId);
        if (id == null || !UserDocumentStore.exists(kind, id)) {
            notifyPlayer("Anchor UI: only locally created documents can be deleted: " + rawId);
            return 0;
        }
        if (kind == UserDocumentStore.Kind.HUD && AnchorHudManager.INSTANCE.isEditing()) {
            notifyPlayer("Anchor UI: close the HUD editor before deleting this HUD");
            return 0;
        }
        try {
            UserDocumentStore.delete(kind, id);
            if (kind == UserDocumentStore.Kind.HUD) {
                AnchorHudManager.INSTANCE.removeUserHudWidgets(id);
            } else if (id.equals(pendingScreen)) {
                pendingScreen = null;
            }
            LoreLoader.INSTANCE.refreshUserDocuments();
            notifyPlayer("Anchor UI: deleted " + kind.name().toLowerCase(java.util.Locale.ROOT) + " '" + UserDocumentStore.display(kind, id) + "'");
            return Command.SINGLE_SUCCESS;
        } catch (IOException | RuntimeException exception) {
            AnchorUI.LOGGER.error("Failed to delete Anchor UI document {}", id, exception);
            LoreLoader.INSTANCE.refreshUserDocuments();
            notifyPlayer("Anchor UI: delete failed: " + exception.getMessage());
            return 0;
        }
    }

    private static int listDocuments(UserDocumentStore.Kind kind) {
        var ids = UserDocumentStore.list(kind);
        notifyPlayer("Anchor UI: " + ids.size() + " locally created " + kind.name().toLowerCase(java.util.Locale.ROOT) + " document(s)");
        for (ResourceLocation id : ids) {
            notifyPlayer(" - " + UserDocumentStore.display(kind, id));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void notifyPlayer(String text) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(text), false);
        }
    }

    private static void openHudEditor() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        if (!(minecraft.screen instanceof AnchorHudEditorScreen)) {
            minecraft.setScreen(new AnchorHudEditorScreen());
        }
    }

    private static int queueScene(TestScene scene) {
        pending = scene;
        return Command.SINGLE_SUCCESS;
    }

    private static int listScenes() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal("Anchor UI — /anchorui test <scene>, animation, rpg, pulse and other HUD groups"), false);
            for (TestScene scene : TestScene.values()) {
                player.displayClientMessage(Component.literal("/anchorui test " + scene.id() + " — " + scene.title()), false);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (pendingScreen != null) {
            ResourceLocation id = pendingScreen;
            pendingScreen = null;
            openPendingScreen(id);
            return;
        }
        if (pendingHudEditor) {
            pendingHudEditor = false;
            openHudEditor();
            return;
        }
        while (KeyRegistry.EDIT_HUD.consumeClick()) {
            if (minecraft.screen == null) {
                openHudEditor();
            }
        }
        if (pending != null) {
            TestScene scene = pending;
            pending = null;
            openScene(scene);
        }
    }

    public static void openScene(TestScene scene) {
        Minecraft minecraft = Minecraft.getInstance();
        LoreEntity entity = LoreLoader.INSTANCE.getData().get(scene.resource());
        if (entity == null) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("Anchor UI: сцена " + scene.id() + " не найдена. Проверь ресурсы и перезагрузи их (F3+T)."), false);
            }
            return;
        }
        minecraft.setScreen(new TestSceneScreen(scene, entity));
    }
}
