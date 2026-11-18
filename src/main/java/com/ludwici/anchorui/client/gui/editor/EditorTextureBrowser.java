package com.ludwici.anchorui.client.gui.editor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Modal selector for PNG resources that really exist in the current client resource packs. */
public final class EditorTextureBrowser {
    private static final int ROW_HEIGHT = 16;
    private static final int TRACK_WIDTH = 8;
    private static final int BACKGROUND = 0xFF162332;
    private static final int BORDER = 0xFF718699;
    private static final int TEXT = 0xFFE4F0F5;
    private static final int MUTED = 0xFFA6BCC9;
    private final ResourceManager resources;
    private final EditorTextureCatalog catalog;
    private final Set<EditorTextureCatalog.PathKey> expanded = new HashSet<>();
    private final Consumer<String> onApply;
    private final Runnable onCancel;
    private final int screenWidth;
    private final int screenHeight;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int bodyTop;
    private final int bodyBottom;
    private final Button apply;
    private final Button cancel;
    private List<EditorTextureCatalog.Row> rows;
    private ResourceLocation selected;
    private int firstRow;
    private boolean draggingScroll;
    private String error = "";

    public EditorTextureBrowser(ResourceManager resources, String initial, int screenWidth, int screenHeight,
                                Consumer<String> onApply, Runnable onCancel) {
        this.resources = resources;
        this.onApply = onApply;
        this.onCancel = onCancel;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.catalog = EditorTextureCatalog.collect(resources);
        width = Math.min(452, Math.max(1, screenWidth - 14));
        height = Math.min(350, Math.max(1, screenHeight - 12));
        x = (screenWidth - width) / 2;
        y = (screenHeight - height) / 2;
        bodyTop = y + 53;
        bodyBottom = Math.max(bodyTop + 1, y + height - 58);
        int buttonWidth = Math.max(1, (width - 22) / 2);
        apply = Button.builder(Component.literal("Use texture"), button -> applySelection()).bounds(x + 6, y + height - 25, buttonWidth, 20).build();
        cancel = Button.builder(Component.literal("Cancel"), button -> onCancel.run()).bounds(x + 16 + buttonWidth, y + height - 25, buttonWidth, 20).build();
        ResourceLocation initialId = ResourceLocation.tryParse(initial);
        if (initialId != null && catalog.contains(initialId)) {
            selected = initialId;
            expanded.addAll(catalog.expandedFor(initialId));
        }
        rows = catalog.visible(expanded);
        if (selected != null) {
            for (int index = 0; index < rows.size(); index++) {
                if (selected.equals(rows.get(index).texture())) {
                    firstRow = Math.max(0, index - Math.max(1, visibleRows()) / 2);
                    break;
                }
            }
        }
        clampScroll();
    }

    public void showError(String message) {
        error = message == null ? "" : message;
    }

    private int visibleRows() {
        return Math.max(1, (bodyBottom - bodyTop) / ROW_HEIGHT);
    }

    private int maxFirstRow() {
        return Math.max(0, rows.size() - visibleRows());
    }

    private void clampScroll() {
        firstRow = Math.max(0, Math.min(firstRow, maxFirstRow()));
    }

    private void toggle(EditorTextureCatalog.PathKey folder) {
        if (!expanded.add(folder)) {
            expanded.remove(folder);
        }
        rows = catalog.visible(expanded);
        clampScroll();
    }

    private void applySelection() {
        if (selected == null || !catalog.contains(selected) || !EditorTextureCatalog.available(resources, selected.toString())) {
            error = "Texture is no longer available in loaded resource packs";
            return;
        }
        onApply.accept(selected.toString());
    }

    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 650.0F);
        try {
            graphics.fill(0, 0, screenWidth, screenHeight, 0xC8000000);
            graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, BORDER);
            graphics.fill(x, y, x + width, y + height, BACKGROUND);
            graphics.drawString(font, "SELECT TEXTURE FROM RESOURCE PACK", x + 8, y + 8, 0xFFFFD16B, false);
            String details = catalog.textureCount() + " available PNG files  |  Click folders to expand";
            graphics.drawString(font, font.plainSubstrByWidth(details, width - 16), x + 8, y + 23, MUTED, false);
            String current = selected == null ? "Select a PNG file" : selected.toString();
            graphics.drawString(font, font.plainSubstrByWidth(current, width - 18), x + 8, y + 37, selected == null ? MUTED : TEXT, false);
            graphics.fill(x + 6, bodyTop - 1, x + width - 6, bodyBottom + 1, 0xFF30465A);
            graphics.fill(x + 7, bodyTop, x + width - 7, bodyBottom, 0xFF101D2A);
            graphics.enableScissor(x + 8, bodyTop, x + width - 8, bodyBottom);
            try {
                int count = Math.min(visibleRows(), rows.size() - firstRow);
                for (int index = 0; index < count; index++) {
                    EditorTextureCatalog.Row row = rows.get(firstRow + index);
                    int rowY = bodyTop + index * ROW_HEIGHT;
                    boolean hovered = mouseX >= x + 8 && mouseX < x + width - 16 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
                    boolean active = row.texture() != null && row.texture().equals(selected);
                    if (hovered || active) {
                        graphics.fill(x + 8, rowY, x + width - 17, rowY + ROW_HEIGHT, hovered ? 0xFF314D65 : 0xFF244859);
                    }
                    int rowX = x + 12 + row.depth() * 13;
                    String prefix = row.directory() ? expanded.contains(row.folder()) ? "[-] " : "[+] " : "    ";
                    String label = prefix + row.label();
                    int available = Math.max(12, x + width - 20 - rowX);
                    graphics.drawString(font, font.plainSubstrByWidth(label, available), rowX, rowY + 4, active ? 0xFFFFD16B : row.directory() ? 0xFF9ADFD8 : TEXT, false);
                }
            } finally {
                graphics.disableScissor();
            }
            drawScrollbar(graphics);
            if (!error.isEmpty()) {
                graphics.drawString(font, font.plainSubstrByWidth(error, width - 16), x + 8, bodyBottom + 7, 0xFFFF9887, false);
            } else {
                graphics.drawString(font, "Only currently loaded, effective PNG assets can be selected", x + 8, bodyBottom + 7, MUTED, false);
            }
            apply.active = selected != null;
            apply.render(graphics, mouseX, mouseY, partialTick);
            cancel.render(graphics, mouseX, mouseY, partialTick);
        } finally {
            graphics.pose().popPose();
        }
    }

    private void drawScrollbar(GuiGraphics graphics) {
        int trackX = x + width - 16;
        graphics.fill(trackX, bodyTop, trackX + TRACK_WIDTH, bodyBottom, 0xFF203345);
        if (maxFirstRow() == 0) {
            return;
        }
        int length = bodyBottom - bodyTop;
        int thumb = Math.max(14, length * visibleRows() / rows.size());
        int thumbY = bodyTop + (length - thumb) * firstRow / maxFirstRow();
        graphics.fill(trackX + 1, thumbY, trackX + TRACK_WIDTH - 1, thumbY + thumb, draggingScroll ? 0xFF9BE3DC : 0xFF668DA5);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (apply.mouseClicked(mouseX, mouseY, button) || cancel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (insideBody(mouseX, mouseY)) {
                if (mouseX >= x + width - 16) {
                    draggingScroll = true;
                    scrollTo(mouseY);
                    return true;
                }
                int row = firstRow + (int) (mouseY - bodyTop) / ROW_HEIGHT;
                if (row >= 0 && row < rows.size()) {
                    EditorTextureCatalog.Row picked = rows.get(row);
                    if (picked.directory()) {
                        toggle(picked.folder());
                    } else {
                        selected = picked.texture();
                        error = "";
                    }
                }
            }
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (draggingScroll && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            scrollTo(mouseY);
        }
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return true;
    }

    private boolean insideBody(double mouseX, double mouseY) {
        return mouseX >= x + 7 && mouseX < x + width - 7 && mouseY >= bodyTop && mouseY < bodyBottom;
    }

    private void scrollTo(double mouseY) {
        if (maxFirstRow() > 0) {
            firstRow = (int) Math.round(Math.max(0, Math.min(1, (mouseY - bodyTop) / Math.max(1, bodyBottom - bodyTop))) * maxFirstRow());
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (insideBody(mouseX, mouseY)) {
            firstRow -= (int) Math.signum(scrollY) * 3;
            clampScroll();
        }
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onCancel.run();
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            applySelection();
        } else if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            firstRow += keyCode == GLFW.GLFW_KEY_UP ? -1 : 1;
            clampScroll();
        }
        return true;
    }
}
