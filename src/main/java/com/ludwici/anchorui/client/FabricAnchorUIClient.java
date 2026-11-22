package com.ludwici.anchorui.client;

import com.ludwici.anchorui.KeyRegistry;
import com.ludwici.anchorui.client.test.AnchorUITestCommands;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

public final class FabricAnchorUIClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyRegistry.register();
        Events.initialize();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(LoreLoader.INSTANCE);
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> com.ludwici.anchorui.client.hud.AnchorHudManager.INSTANCE.render(graphics, tickDelta));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> AnchorUITestCommands.register(dispatcher));
        ClientTickEvents.END_CLIENT_TICK.register(AnchorUITestCommands::onClientTick);
    }
}
