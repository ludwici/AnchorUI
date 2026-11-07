package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.AnchorUI;
import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.client.LoreLoader;
import com.mojang.brigadier.Command;
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

    private AnchorUITestCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        var test = Commands.literal("test")
                .executes(context -> listScenes())
                .then(Commands.literal("list").executes(context -> listScenes()));

        for (TestScene scene : TestScene.values()) {
            test.then(Commands.literal(scene.id()).executes(context -> queueScene(scene)));
        }

        event.getDispatcher().register(Commands.literal("anchorui").then(test));
    }

    private static int queueScene(TestScene scene) {
        pending = scene;
        return Command.SINGLE_SUCCESS;
    }

    private static int listScenes() {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal("Anchor UI — /anchorui test <scene1..scene13>"), false);
            for (TestScene scene : TestScene.values()) {
                player.displayClientMessage(Component.literal("/anchorui test " + scene.id() + " — " + scene.title()), false);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (pending == null) {
            return;
        }
        TestScene scene = pending;
        pending = null;
        openScene(scene);
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
