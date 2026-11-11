package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.KeyRegistry;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.client.LoreLoader;
import com.ludwici.anchorui.client.ScreenIds;
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

@EventBusSubscriber(modid = AnchorUI.MODID, value = Dist.CLIENT)
public final class AnchorUITestCommands {
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
        registerHudTest(test, "binding");

        for (TestScene scene : TestScene.values()) {
            test.then(Commands.literal(scene.id()).executes(context -> queueScene(scene)));
        }

        var hud = Commands.literal("hud")
                .then(Commands.literal("edit").executes(context -> queueHudEditor()));
        var screens = Commands.literal("screen")
                .then(Commands.literal("open")
                        .then(Commands.argument("id", StringArgumentType.greedyString())
                                .suggests((context, builder) -> {
                                    LoreLoader.INSTANCE.getData().keySet().stream()
                                            .filter(id -> id.getPath().startsWith("screens/"))
                                            .map(ScreenIds::display).sorted().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> queueScreen(StringArgumentType.getString(context, "id")))));
        event.getDispatcher().register(Commands.literal("anchorui").then(test).then(hud).then(screens));
    }

    private static void registerHudTest(com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> test, String group) {
        test.then(Commands.literal(group)
                .executes(context -> setHud(group, !AnchorHudManager.INSTANCE.isTestEnabled(group)))
                .then(Commands.literal("on").executes(context -> setHud(group, true)))
                .then(Commands.literal("off").executes(context -> setHud(group, false))));
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
        pendingHudEditor = true;
        return Command.SINGLE_SUCCESS;
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
            player.displayClientMessage(Component.literal("Anchor UI — /anchorui test <scene1..scene14>, hud1, rpg or binding"), false);
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
