package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.KeyRegistry;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.api.animation.UiAnimationTriggerRegistry;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.ScreenIds;
import com.ludwici.anchorui.client.UserUiStorage;
import com.ludwici.anchorui.client.hud.AnchorHudManager;
import com.ludwici.anchorui.client.hud.AnchorHudEditorScreen;
import com.ludwici.anchorui.client.gui.ItemLoreScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.network.chat.Component;
import java.util.concurrent.atomic.AtomicLong;
import java.io.IOException;

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

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        var test = ClientCommandManager.literal("test")
                .executes(context -> listScenes())
                .then(ClientCommandManager.literal("list").executes(context -> listScenes()));

        registerHudTest(test, "hud1");
        registerHudTest(test, "rpg");
        registerHudTest(test, "animation");
        test.then(ClientCommandManager.literal("pulse").executes(context -> testPulse()));
        registerHudTest(test, "binding");
        registerHudTest(test, "conditions");
        registerHudTest(test, "vanilla");

        for (TestScene scene : TestScene.values()) {
            test.then(ClientCommandManager.literal(scene.id()).executes(context -> queueScene(scene)));
        }

        var hud = ClientCommandManager.literal("hud");
        var hudEdit = ClientCommandManager.literal("edit").executes(context -> queueHudEditor());
        hudEdit.then(ClientCommandManager.argument("id", StringArgumentType.greedyString()).suggests((context, builder) -> suggestHuds(builder)).executes(context -> queueHudEditor(StringArgumentType.getString(context, "id"))));
        hud.then(hudEdit);
        var hudCreate = ClientCommandManager.literal("create");
        hudCreate.then(ClientCommandManager.argument("id", StringArgumentType.greedyString()).executes(context -> createUi(UserUiStorage.Kind.HUD, StringArgumentType.getString(context, "id"))));
        hud.then(hudCreate);
        var hudDelete = ClientCommandManager.literal("delete");
        hudDelete.then(ClientCommandManager.argument("id", StringArgumentType.greedyString()).suggests((context, builder) -> suggestLocal(UserUiStorage.Kind.HUD, builder)).executes(context -> deleteUi(UserUiStorage.Kind.HUD, StringArgumentType.getString(context, "id"))));
        hud.then(hudDelete);
        hud.then(ClientCommandManager.literal("list").executes(context -> listUi(UserUiStorage.Kind.HUD)));

        var screens = ClientCommandManager.literal("screen");
        var openScreen = ClientCommandManager.literal("open");
        openScreen.then(ClientCommandManager.argument("id", StringArgumentType.greedyString()).suggests((context, builder) -> {
            LoreLoader.INSTANCE.getData().keySet().stream().filter(id -> id.getPath().startsWith("screens/")).map(ScreenIds::display).sorted().forEach(builder::suggest);
            return builder.buildFuture();
        }).executes(context -> queueScreen(StringArgumentType.getString(context, "id"))));
        screens.then(openScreen);
        var screenCreate = ClientCommandManager.literal("create");
        screenCreate.then(ClientCommandManager.argument("id", StringArgumentType.greedyString()).executes(context -> createUi(UserUiStorage.Kind.SCREEN, StringArgumentType.getString(context, "id"))));
        screens.then(screenCreate);
        var screenDelete = ClientCommandManager.literal("delete");
        screenDelete.then(ClientCommandManager.argument("id", StringArgumentType.greedyString()).suggests((context, builder) -> suggestLocal(UserUiStorage.Kind.SCREEN, builder)).executes(context -> deleteUi(UserUiStorage.Kind.SCREEN, StringArgumentType.getString(context, "id"))));
        screens.then(screenDelete);
        screens.then(ClientCommandManager.literal("list").executes(context -> listUi(UserUiStorage.Kind.SCREEN)));
        dispatcher.register(ClientCommandManager.literal("anchorui").then(test).then(hud).then(screens));
    }

    private static void registerHudTest(com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> test, String group) {
        test.then(ClientCommandManager.literal(group).executes(context -> setHud(group, !AnchorHudManager.INSTANCE.isTestEnabled(group))).then(ClientCommandManager.literal("on").executes(context -> setHud(group, true))).then(ClientCommandManager.literal("off").executes(context -> setHud(group, false))));
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
        AnchorHudManager.INSTANCE.selectDefaultHud();
        pendingHudEditor = true;
        return Command.SINGLE_SUCCESS;
    }

    private static int queueHudEditor(String rawId) {
        ResourceLocation id = UserUiStorage.resolve(UserUiStorage.Kind.HUD, rawId);
        if (id == null || !LoreLoader.INSTANCE.getHudDocuments().containsKey(id)) {
            notifyPlayer("Anchor UI: HUD '" + rawId + "' was not found");
            return 0;
        }
        AnchorHudManager.INSTANCE.setActiveUserHud(id);
        pendingHudEditor = true;
        return Command.SINGLE_SUCCESS;
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestHuds(com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (ResourceLocation id : AnchorHudManager.INSTANCE.getAvailableHuds()) {
            builder.suggest(UserUiStorage.display(UserUiStorage.Kind.HUD, id));
        }
        return builder.buildFuture();
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestLocal(
            UserUiStorage.Kind kind, com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        for (ResourceLocation id : UserUiStorage.list(kind)) {
            builder.suggest(UserUiStorage.display(kind, id));
        }
        return builder.buildFuture();
    }

    private static int createUi(UserUiStorage.Kind kind, String rawId) {
        ResourceLocation id = UserUiStorage.resolve(kind, rawId);
        if (id == null) {
            notifyPlayer("Anchor UI: ID must be namespace:name");
            return 0;
        }
        boolean registered = kind == UserUiStorage.Kind.HUD
                ? LoreLoader.INSTANCE.getHudDocuments().containsKey(id)
                : LoreLoader.INSTANCE.getData().containsKey(id);
        if (registered || UserUiStorage.exists(kind, id)) {
            notifyPlayer("Anchor UI: " + kind.name().toLowerCase(java.util.Locale.ROOT) + " '" + UserUiStorage.display(kind, id) + "' already exists");
            return 0;
        }
        try {
            UserUiStorage.create(kind, id);
            LoreLoader.INSTANCE.refreshUserDocuments();
            registered = kind == UserUiStorage.Kind.HUD
                    ? LoreLoader.INSTANCE.getHudDocuments().containsKey(id)
                    : LoreLoader.INSTANCE.getData().containsKey(id);
            if (!registered) {
                UserUiStorage.delete(kind, id);
                LoreLoader.INSTANCE.refreshUserDocuments();
                notifyPlayer("Anchor UI: new " + kind.name().toLowerCase(java.util.Locale.ROOT) + " could not be loaded");
                return 0;
            }
            if (kind == UserUiStorage.Kind.HUD) {
                AnchorHudManager.INSTANCE.setActiveUserHud(id);
                pendingHudEditor = true;
            } else {
                pendingScreen = id;
            }
            notifyPlayer("Anchor UI: created " + kind.name().toLowerCase(java.util.Locale.ROOT) + " '" + UserUiStorage.display(kind, id) + "'");
            return Command.SINGLE_SUCCESS;
        } catch (IOException | RuntimeException exception) {
            AnchorUI.LOGGER.error("Failed to create Anchor UI {}", kind, exception);
            notifyPlayer("Anchor UI: create failed: " + exception.getMessage());
            return 0;
        }
    }

    private static int deleteUi(UserUiStorage.Kind kind, String rawId) {
        ResourceLocation id = UserUiStorage.resolve(kind, rawId);
        if (id == null || !UserUiStorage.exists(kind, id)) {
            notifyPlayer("Anchor UI: only locally created HUDs and Screens can be deleted: " + rawId);
            return 0;
        }
        if (kind == UserUiStorage.Kind.HUD && AnchorHudManager.INSTANCE.isEditing()) {
            notifyPlayer("Anchor UI: close the HUD editor before deleting this HUD");
            return 0;
        }
        try {
            UserUiStorage.delete(kind, id);
            if (kind == UserUiStorage.Kind.HUD) {
                AnchorHudManager.INSTANCE.removeUserHudWidgets(id);
            } else if (id.equals(pendingScreen)) {
                pendingScreen = null;
            }
            LoreLoader.INSTANCE.refreshUserDocuments();
            notifyPlayer("Anchor UI: deleted " + kind.name().toLowerCase(java.util.Locale.ROOT) + " '" + UserUiStorage.display(kind, id) + "'");
            return Command.SINGLE_SUCCESS;
        } catch (IOException | RuntimeException exception) {
            AnchorUI.LOGGER.error("Failed to delete Anchor UI {}", kind, exception);
            LoreLoader.INSTANCE.refreshUserDocuments();
            notifyPlayer("Anchor UI: delete failed: " + exception.getMessage());
            return 0;
        }
    }

    private static int listUi(UserUiStorage.Kind kind) {
        var ids = kind == UserUiStorage.Kind.HUD
                ? AnchorHudManager.INSTANCE.getAvailableHuds()
                : LoreLoader.INSTANCE.getData().keySet().stream().filter(id -> id.getPath().startsWith("screens/")).sorted(java.util.Comparator.comparing(ResourceLocation::toString)).toList();
        notifyPlayer("Anchor UI: " + ids.size() + " " + (kind == UserUiStorage.Kind.HUD ? "HUD(s)" : "Screen(s)"));
        for (ResourceLocation id : ids) {
            notifyPlayer(" - " + UserUiStorage.display(kind, id) + (UserUiStorage.exists(kind, id) ? " (local)" : " (resource pack)"));
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

    public static void onClientTick(Minecraft minecraft) {
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
