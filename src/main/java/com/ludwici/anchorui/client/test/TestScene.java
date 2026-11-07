package com.ludwici.anchorui.client.test;

import com.ludwici.anchorui.AnchorUI;
import net.minecraft.resources.ResourceLocation;

public enum TestScene {
    SCENE1("scene1", "Девять точек привязки", "Все девять якорей должны оставаться у своих краёв и в центре"),
    SCENE2("scene2", "Цепочки привязок", "grandchild: TOPLEFT к RIGHT.BOTTOMLEFT + (0, 7), независимо от порядка JSON"),
    SCENE3("scene3", "Размеры и оформление", "Offset от primary; сравни Alpha 27%, 53% и 100% одного цвета"),
    SCENE4("scene4", "Текст и модель", "Проверь статичную модель, перенос строк, форматирование и подсказку"),
    SCENE5("scene5", "Изменение размера", "Измени размер окна или масштаб GUI: блоки должны перестроиться"),
    SCENE6("scene6", "Скрытые элементы", "Нажми G: невидимый якорь появится только в режиме контуров"),
    SCENE7("scene7", "Неизвестная ссылка", "Ссылка missing_target заменяется screen; предупреждение в консоли"),
    SCENE8("scene8", "Циклические якоря", "Ожидается сообщение Circular frame anchors, без вылета"),
    SCENE9("scene9", "Ванильный StringWidget", "Выравнивание, вертикальный центр, padding и многоточие"),
    SCENE10("scene10", "Теги и многострочность", "Жирный, курсив, подчёркивание, подсказка, перенос и явные строки"),
    SCENE11("scene11", "Пользовательские виджеты", "Кликни по двум счётчикам, зарегистрированным через публичный WidgetFactory"),
    SCENE12("scene12", "Ошибки фабрик", "Незнакомый тип и ошибка фабрики не должны скрывать остальные элементы"),
    SCENE13("scene13", "Record + Codec", "Значения по умолчанию, проверка полей и обработка неверной конфигурации"),
    SCENE14("scene14", "Неймспейсы и импорт", "Якорь на виджет другого namespace и импорт описания чужого виджета");

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
