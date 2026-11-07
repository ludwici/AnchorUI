package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.api.LoreEntity;
import com.ludwici.anchorui.client.gui.ItemLoreScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public class TestSceneScreen extends ItemLoreScreen {
    private final TestScene scene;
    private boolean guides;

    public TestSceneScreen(TestScene scene, LoreEntity entity) {
        super(null, ItemStack.EMPTY, entity);
        this.scene = scene;
    }

    @Override
    protected void init() {
        super.init();
        int center = width / 2;
        addRenderableWidget(Button.builder(Component.literal("<"), button -> AnchorUITestCommands.openScene(scene.previous())).bounds(center - 95, height - 26, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), button -> onClose()).bounds(center - 62, height - 26, 124, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> AnchorUITestCommands.openScene(scene.next())).bounds(center + 67, height - 26, 28, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xDE101722);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (guides) {
            graphics.fill(width / 2, 0, width / 2 + 1, height, 0x6666DDEE);
            graphics.fill(0, height / 2, width, height / 2 + 1, 0x6666DDEE);
            for (var entry : getLayoutBounds().entrySet()) {
                if (entry.getKey().equals("screen")) {
                    continue;
                }
                var bounds = entry.getValue();
                boolean hiddenAnchor = scene == TestScene.SCENE6 && entry.getKey().equals("invisible");
                int color = hiddenAnchor ? 0xFFFF8098 : 0xFF52D9E5;
                String label = hiddenAnchor ? "invisible (hidden)" : entry.getKey();
                outline(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height(), color);
                graphics.drawString(font, label, bounds.x(), bounds.y() - 9, color, false);
            }
        }
        graphics.fill(0, 0, width, 33, 0xEC0C1019);
        graphics.drawString(font, "Anchor UI · " + scene.id() + " · " + scene.title(), 7, 5, 0xFFFFD37C, false);
        graphics.drawString(font, scene.description(), 7, 18, 0xFFD1D8E1, false);
        String error = getLayoutError();
        if (error != null) {
            int y = Math.max(40, height / 2 - 15);
            graphics.fill(8, y - 7, width - 8, y + 30, 0xCC5C1722);
            graphics.drawString(font, "Ошибка компоновки:", 16, y, 0xFFFFC0C0, false);
            graphics.drawString(font, error, 16, y + 13, 0xFFFFFFFF, false);
        }
        if (!getLayoutWarnings().isEmpty()) {
            graphics.fill(8, 37, width - 8, 62, 0xCC5A4824);
            graphics.drawString(font, "WARN (" + getLayoutWarnings().size() + "): " + getLayoutWarnings().getFirst(), 16, 45, 0xFFFFD080, false);
        }
        graphics.fill(0, height - 32, width, height, 0xEC0C1019);
        graphics.drawString(font, "G — контуры | Esc — закрыть", 7, height - 21, 0xFFBAC7D9, false);
    }

    private void outline(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_G) {
            guides = !guides;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
