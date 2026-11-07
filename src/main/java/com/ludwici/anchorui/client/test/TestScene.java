package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.AnchorUI;
import net.minecraft.resources.ResourceLocation;

public enum TestScene {
    SCENE1("scene1", "Девять точек привязки", "Все девять якорей должны оставаться у своих краёв и в центре"),
    SCENE2("scene2", "Цепочки привязок", "Элементы связаны друг с другом, включая обратный порядок JSON"),
    SCENE3("scene3", "Размеры и оформление", "Проверь размеры блоков, отступы, прозрачность и цвета текста"),
    SCENE4("scene4", "Текст и модель", "Проверь перенос текста, форматирование, подсказку и перетаскивание модели"),
    SCENE5("scene5", "Изменение размера", "Измени размер окна или масштаб GUI: блоки должны перестроиться"),
    SCENE6("scene6", "Скрытые элементы", "Невидимый якорь сохраняет положение зависимых элементов"),
    SCENE7("scene7", "Неизвестная ссылка", "Ожидается сообщение Unknown relativeTo frame, без вылета"),
    SCENE8("scene8", "Циклические якоря", "Ожидается сообщение Circular frame anchors, без вылета");

    private static final TestScene[] VALUES = values();

    private final String id;
    private final String title;
    private final String description;

    TestScene(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public ResourceLocation resource() {
        return ResourceLocation.fromNamespaceAndPath(AnchorUI.MODID, "test/scenes/" + id);
    }

    public TestScene next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public TestScene previous() {
        return VALUES[(ordinal() + VALUES.length - 1) % VALUES.length];
    }
}
