package com.ludwici.anchorui.client.gui.widgets;

import com.ludwici.anchorui.client.api.types.StyledComponent;
import com.ludwici.anchorui.client.gui.ItemLoreScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AbstractLoreWidget<T extends StyledComponent> extends AbstractWidget {
    
    public T base;
    public ItemLoreScreen screen;
    
    private AbstractLoreWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage) {
        super(pX, pY, pWidth, pHeight, pMessage);
    }
    
    public AbstractLoreWidget(T component, ItemLoreScreen screen) {
        super(component.getX(), component.getY(), 0, 0, Component.empty());
        base = component;
        this.screen = screen;
    }

    private ScreenRectangle getAnchorRectangle() {
        var children = screen.children();
        for (var c : children) {
            if (c instanceof AbstractLoreWidget<?> widget && widget != this) {
                if (widget.base.id.equals(base.position.relativeTo)) {
                    return widget.getRectangle();
                }
            }
        }
        return screen.getRectangle();
    }

    @Override
    public int getX() {
        int baseX = super.getX();

        ScreenRectangle rect = getAnchorRectangle();

        int anchorX;
        switch (base.position.anchor) {
            case TOPLEFT, LEFT, BOTTOMLEFT -> {
                anchorX = rect.left();
            }
            case TOP, CENTER, BOTTOM -> {
                anchorX = rect.left() + rect.width() / 2;
            }
            case TOPRIGHT, RIGHT, BOTTOMRIGHT -> {
                anchorX = rect.left() + rect.width();
            }
            default -> anchorX = rect.left();
        }

        int finalX;
        switch (base.position.point) {
            case TOPLEFT, LEFT, BOTTOMLEFT -> {
                finalX = anchorX + baseX;
            }
            case TOP, CENTER, BOTTOM -> {
                finalX = anchorX + baseX - getWidth() / 2;
            }
            case TOPRIGHT, RIGHT, BOTTOMRIGHT -> {
                finalX = anchorX - baseX - getWidth();
            }
            default -> finalX = anchorX + baseX;
        }

        return finalX;
    }

    @Override
    public int getY() {
        int baseY = super.getY();

        ScreenRectangle rect = getAnchorRectangle();

        int anchorY;
        switch (base.position.anchor) {
            case TOPLEFT, TOP, TOPRIGHT -> {
                anchorY = rect.top();
            }
            case LEFT, CENTER, RIGHT -> {
                anchorY = rect.top() + rect.height() / 2;
            }
            case BOTTOMLEFT, BOTTOM, BOTTOMRIGHT -> {
                anchorY = rect.top() + rect.height();
            }
            default -> anchorY = rect.top();
        }

        int finalY;
        switch (base.position.point) {
            case TOPLEFT, TOP, TOPRIGHT -> {
                finalY = anchorY + baseY;
            }
            case LEFT, CENTER, RIGHT -> {
                finalY = anchorY + baseY - getHeight() / 2;
            }
            case BOTTOMLEFT, BOTTOM, BOTTOMRIGHT -> {
                finalY = anchorY - baseY - getHeight();
            }
            default -> finalY = anchorY + baseY;
        }

        return finalY;
    }
}
