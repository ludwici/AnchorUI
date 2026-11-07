package com.ludwici.anchorui.client.gui;

import com.ludwici.anchorui.api.LorePiece;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import java.util.List;

public class ItemLoreListWidget extends AbstractWidget {
    private ItemLoreScreen screen;

    private List<Component> content = List.of(
            Component.literal("Толпа людей с воздетыми руками. Они, вроде, что-то несут. Точнее разобрать не получится, пока не найду проектор..."),
            Component.literal("Валамбуа недавно менял паспорт... Неужели он рассчитывал вернуться в Амерзон?")
    );

    public ItemLoreListWidget(int x, int y, ItemLoreScreen screen) {
        super(x, y, 100, 200, Component.empty());
        this.screen = screen;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(getX(), getY(), getRight(), getBottom(), -10,0x80000000);

        Minecraft minecraft = Minecraft.getInstance();
        int x = 2;
        int y = 2;
        int paddingX = 2;
        int paddingY = 2;

        Font font = minecraft.font;

        int curHeight = paddingY;

        for (var lore: screen.loreEntity.components) {
            String text;
            int lineWidth = width - paddingX * 2;

            if (lore.hidden) {
                text = "?????? ???? ?????????";
                guiGraphics.drawWordWrap(minecraft.font, FormattedText.of(text), getX() + paddingX, y, width - paddingX * 2, 0xFFE278);
            } else {
//                List<LorePiece> pieces = LoreParser.parse(lore.descSource);
                int curWidth = 0;
                int locY = y;
                int locX = getX() + paddingX;

                for (LorePiece p : lore.pieces) {
                    text = p.text;

                    String[] words = text.split(" ");
                    for (String word : words) {
                        if (word.isEmpty()) {
                            continue;
                        }
                        var wComp = Component.literal(word).withStyle(p.getStyle());
                        int wWidth = font.width(wComp);
                        if (curWidth + wWidth > lineWidth) {
                            locX = getX() + paddingX;
                            locY += 9;
                            curHeight += font.wordWrapHeight(wComp, lineWidth);
                            curWidth = 0;
                        }
                        if (mouseX >= locX && mouseY >= locY && mouseX <= (locX + wWidth) && mouseY <= (locY + 9)) {
                            guiGraphics.renderComponentHoverEffect(font, p.getStyle(), mouseX, mouseY);
                        }
                        guiGraphics.drawString(font, wComp, locX, locY, 0xFFE278, true);
                        locX += wWidth + font.width(" ");
                        curWidth += wWidth + font.width(" ");
                    }
                }

            }

            y+= curHeight + 8;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
