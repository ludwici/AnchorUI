# Собственные виджеты в Anchor UI

Ветку `1.21.1` можно использовать для расширения набора JSON-виджетов сторонними модами. Тип идентифицируется полным `ResourceLocation`, например `examplemod:counter`. Клиент с таким модом должен иметь установленный Anchor UI.

## 1. Подключение

Подключи Anchor UI как зависимость для компиляции и укажи обязательную зависимость `anchorui` в метаданных своего мода. Регистрацию выполняй только на клиентской стороне во время `FMLClientSetupEvent`; на сервере виджеты не регистрируются.

## 2. Регистрация фабрики

```java
@EventBusSubscriber(modid = "examplemod", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation type = ResourceLocation.fromNamespaceAndPath("examplemod", "counter");
            AnchorWidgetRegistry.register(type, (context, element, style) -> {
                int initial = GsonHelper.getAsInt(element.settings, "initial", 0);
                int step = GsonHelper.getAsInt(element.settings, "step", 1);
                AtomicInteger value = new AtomicInteger(initial);
                return Button.builder(Component.literal("Счётчик: " + initial),
                        button -> button.setMessage(Component.literal("Счётчик: " + value.addAndGet(step))))
                        .bounds(0, 0, 100, 20).build();
            });
        });
    }
}
```

Необходимые классы импортируются из NeoForge, Minecraft, `java.util.concurrent.atomic.AtomicInteger` и публичного API Anchor UI:

- `com.ludwici.anchorui.api.widget.AnchorWidgetRegistry`
- `com.ludwici.anchorui.api.widget.WidgetFactory`
- `com.ludwici.anchorui.api.widget.WidgetContext`

Наследоваться от `AnchoredWidget` не требуется. В примере создаётся обычный `Button` из Minecraft. Регистрируй тип ровно один раз; повторная регистрация одного ID вызывает `IllegalStateException`, чтобы не позволять модам молча заменять виджеты друг друга.

## 3. JSON

```json
{
  "type": "examplemod:counter",
  "id": "mana_counter",
  "initial": 10,
  "step": 5,
  "position": {
    "point": "CENTER",
    "anchor": "CENTER",
    "relativeTo": "screen",
    "x": 0,
    "y": 0
  },
  "size": {
    "width": 140,
    "height": 26
  }
}
```

`type`, `id`, `position`, `size` и `hidden` обрабатывает Anchor UI. Остальные поля фабрика может читать из `element.settings` (это полный `JsonObject` элемента).

## 4. Контекст и стили

`WidgetFactory` получает:

- `WidgetContext`: `minecraft()`, `screen()`, `item()`, `entity()`, `font()` и `resolveItem(String)`.
- `LoreEntity.UiElement`: `type`, `id`, `text`, `path`, `scale`, `lore`, `settings`.
- `LoreStyle.Element`: `position`, `size`, `padding`, `background`, `textColor`, `textAlign`, `textWrap`, `hidden`.

Anchor UI автоматически выставляет координаты, явно заданные ширину и высоту, видимость и добавляет `AbstractWidget` в `Screen`. Ванильная обработка кликов, наведения и фокуса продолжает работать.

Для `AbstractWidget`, который не наследуется от `AnchoredWidget`, фоновый цвет Anchor UI рисуется **позади** виджета, если он задан. У самих ванильных кнопок может быть собственный непрозрачный фон. `padding` передаётся фабрике в `style`, но требует явной обработки внутри собственного виджета; это не универсальный автоматический отступ для любого ванильного компонента.

Ширина и высота `0` означают, что размеры определяет сам виджет. Когда JSON указывает положительные `size.width` и `size.height`, Anchor UI применяет их после вызова фабрики.

## 5. Ошибки и изоляция

При неизвестном `type` виджет пропускается, а в журнал выводится `WARN`. Если фабрика бросает `RuntimeException`, Anchor UI записывает `ERROR` со стеком, пропускает только проблемный виджет и продолжает собирать остальной экран. Возврат `null` тоже пропускается с предупреждением.

Порядок размещения соседних элементов сохраняется: описания фреймов и созданные виджеты хранятся вместе. Если `relativeTo` указывает на несуществующий или пропущенный виджет, используется существующий механизм резервной привязки к `screen` с `WARN`. Циклы между существующими элементами продолжают считаться ошибкой компоновки.

## 6. Тестирование

`/anchorui test scene11` — два работающих `anchorui_test:counter` через публичную фабрику.

`/anchorui test scene12` — незарегистрированный тип и исключение фабрики, остальные элементы продолжают отображаться.

Для следующих этапов остаются отдельные задачи: универсальные вложенные контейнеры, произвольные `Renderable`, реактивные данные и обновление динамических виджетов.
