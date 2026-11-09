# Собственные виджеты в Anchor UI

Ветку `1.21.1` можно использовать для расширения набора JSON-виджетов сторонними модами. Тип идентифицируется полным `ResourceLocation`, например `examplemod:counter`. Клиент с таким модом должен иметь установленный Anchor UI.

## 1. Подключение

Подключи Anchor UI как зависимость для компиляции и укажи обязательную зависимость `anchorui` в метаданных своего мода. Регистрацию выполняй только на клиентской стороне во время `FMLClientSetupEvent`; на сервере виджеты не регистрируются.

## 2. Типизированная конфигурация через Java record и Codec

Для собственного виджета удобно объявить конфигурацию как Java `record`. `Codec<T>` из Minecraft DataFixerUpper описывает JSON-поля, их типы, значения по умолчанию и дополнительные ограничения.

Например, счётчик принимает `initial` и `step`:

```java
public record CounterConfig(int initial, int step) {
    private static final Codec<CounterConfig> BASE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("initial", 0).forGetter(CounterConfig::initial),
            Codec.INT.optionalFieldOf("step", 1).forGetter(CounterConfig::step)
    ).apply(instance, CounterConfig::new));

    public static final Codec<CounterConfig> CODEC = BASE_CODEC.validate(config -> {
        if (config.step() <= 0) {
            return DataResult.error(() -> "step must be greater than zero");
        }
        return DataResult.success(config);
    });
}
```

Используются `com.mojang.serialization.Codec`, `DataResult` и `com.mojang.serialization.codecs.RecordCodecBuilder`.

**Регистрация фабрики:** рядом с идентификатором типа передаём `CounterConfig.CODEC`. Anchor UI автоматически декодирует JSON до вызова фабрики и передаёт в неё готовый `CounterConfig`.

```java
@EventBusSubscriber(modid = "examplemod", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ResourceLocation type = ResourceLocation.fromNamespaceAndPath("examplemod", "counter");
            AnchorWidgetRegistry.register(type, CounterConfig.CODEC, (context, element, style, config) -> {
                AtomicInteger value = new AtomicInteger(config.initial());
                return Button.builder(Component.literal("Счётчик: " + config.initial()), button -> button.setMessage(Component.literal("Счётчик: " + value.addAndGet(config.step())))).bounds(0, 0, 100, 20).build();
            });
        });
    }
}
```

Для примера также потребуются импорты `java.util.concurrent.atomic.AtomicInteger`, `com.ludwici.anchorui.api.widget.AnchorWidgetRegistry` и стандартных классов NeoForge и Minecraft.

Типизация распространяется и на вложенные объекты: можно определить вложенный record, например `AnimationConfig`, создать для него свой `Codec<AnimationConfig>` и включить его через `AnimationConfig.CODEC.fieldOf("animation")` в основной `RecordCodecBuilder`.

**Совместимость:** старый `AnchorWidgetRegistry.register(type, WidgetFactory)` остаётся доступным. В нём сторонний код может самостоятельно читать `element.settings` как `JsonObject`. Для новых виджетов рекомендуется перегрузка с `Codec<T>` и `ConfiguredWidgetFactory<T>`.

При повторной регистрации того же ID возникает `IllegalStateException`. Наследоваться от `AnchoredWidget` не требуется: фабрика может вернуть обычный `AbstractWidget`.

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

Общие поля `type`, `id`, `position`, `size`, `hidden` обрабатывает Anchor UI. `CounterConfig.CODEC` читает только `initial` и `step`: лишние поля интерфейса не мешают. Пропущенные `initial` и `step` получают значения `0` и `1`.

Например, `"step": "slow"` нарушает тип, а `"step": 0` нарушает правило из `validate`. Такой виджет пропускается с предупреждением, где указаны тип, ID и ошибка декодирования.

## 4. Контекст и стили

Обычная `WidgetFactory` получает три параметра. `ConfiguredWidgetFactory<T>` получает те же параметры плюс четвёртый — типизированный `config`:

- `WidgetContext`: `minecraft()`, `screen()`, `item()`, `entity()`, `font()` и `resolveItem(String)`.
- `LoreEntity.UiElement`: `type`, `id`, `text`, `path`, `scale`, `lore`, `settings`.
- `LoreStyle.Element`: `position`, `size`, `padding`, `background`, `textColor`, `textAlign`, `textWrap`, `hidden`.

Anchor UI автоматически выставляет координаты, явно заданные ширину и высоту, видимость и добавляет `AbstractWidget` в `Screen`. Ванильная обработка кликов, наведения и фокуса продолжает работать.

Для `AbstractWidget`, который не наследуется от `AnchoredWidget`, фоновый цвет Anchor UI рисуется **позади** виджета, если он задан. У самих ванильных кнопок может быть собственный непрозрачный фон. `padding` передаётся фабрике в `style`, но требует явной обработки внутри собственного виджета; это не универсальный автоматический отступ для любого ванильного компонента.

Ширина и высота `0` означают, что размеры определяет сам виджет. Когда JSON указывает положительные `size.width` и `size.height`, Anchor UI применяет их после вызова фабрики.

## 5. Ошибки и изоляция

При ошибке в `Codec<T>` Anchor UI создаёт `WidgetConfigException`, пишет `WARN` с названием типа, ID элемента и описанием ошибки, пропускает этот виджет и продолжает создание остальных. При этом пользовательская фабрика вообще не вызывается для неверной конфигурации.

При неизвестном `type` виджет пропускается, а в журнал выводится `WARN`. Если фабрика бросает `RuntimeException`, Anchor UI записывает `ERROR` со стеком, пропускает только проблемный виджет и продолжает собирать остальной экран. Возврат `null` тоже пропускается с предупреждением.

Порядок размещения соседних элементов сохраняется: описания фреймов и созданные виджеты хранятся вместе. Если `relativeTo` указывает на несуществующий или пропущенный виджет, используется существующий механизм резервной привязки к `screen` с `WARN`. Циклы между существующими элементами продолжают считаться ошибкой компоновки.

## 6. Тестирование

`/anchorui test scene11` — два работающих `anchorui_test:counter` через публичную фабрику.

`/anchorui test scene12` — незарегистрированный тип и исключение фабрики, остальные элементы продолжают отображаться.

`/anchorui test scene13` — запись `CounterConfig`, значения по умолчанию и неверные значения `step`. Корректные счётчики работают, ошибочные пропускаются с `WARN`.

Для следующих этапов остаются отдельные задачи: универсальные вложенные контейнеры, произвольные `Renderable`, реактивные данные и обновление динамических виджетов.

## 7. Идентификаторы экземпляров и ссылки между модами

Тип (например, `anchorui:text`) описывает фабрику, а `id` (например, `othermod:header`) — конкретный элемент на экране. Они имеют независимые пространства имён. Неуточнённые значения `id` и `position.relativeTo` получают неймспейс ресурса экрана; `relativeTo: "screen"` остаётся специальным значением.

Подключаемые виджеты другого мода должны быть опубликованы как шаблоны в `assets/<namespace>/anchorui/widgets/<name>.json`. В своём документе можно использовать `"imports": ["othermod:header"]` и после этого указывать `"relativeTo": "othermod:header"`. Альтернативно, в `entities` доступен `"ref": "othermod:header"` с локальными переопределениями, в том числе с новым `id`.

Anchor UI загружает все описания и регистрирует их перед созданием экранов. Затем он собирает все определения элементов текущего экрана, вызывает фабрики и разрешает якоря. `WidgetContext.widgets()` позволяет найти объявление по полному ID с помощью `definition(id)` или уже созданный виджет через `widget(id)`. Во время вызова фабрики остальные экземпляры ещё могут отсутствовать, но все объявления уже зарегистрированы.

Каталог ресурсов доступен через `LoreLoader.INSTANCE.getDefinitions()`; `find(ResourceLocation)` возвращает копию JSON-описания. Все определения перечитываются при перезагрузке ресурсов, а экземпляры привязаны к конкретному открытому экрану.

Пример структуры каталогов и правила импортов приведены в [WIDGET_REFERENCES.md](WIDGET_REFERENCES.md). Игровая проверка — `/anchorui test scene14`.

## 8. HUD widget factories

Anchor UI's single NeoForge HUD layer invokes the **same** `WidgetFactory` and `ConfiguredWidgetFactory<T>` registrations used by `ItemLoreScreen`. HUD JSON lives under `assets/<namespace>/anchorui/hud/<name>.json` and can use `imports`, `ref`, namespaced `id` and `position.relativeTo`. All HUD documents contributed by mods are composed into one layout.

When called from a HUD, `WidgetContext.screen()` returns `null` because no normal `Screen` is open. Factories should rely on `context.minecraft()`, `context.font()`, `context.widgets()`, etc., and handle the nullable screen properly. The HUD supplies an empty `ItemStack` as the default item; an `anchorui:model` widget can specify `path` to display a particular item.

HUD widgets are currently visual only: the manager renders `AbstractWidget` with off-screen mouse coordinates, leaving click/hover/keyboard routing to the future HUD editor. Vanilla buttons can render but cannot be clicked on the ordinary game HUD.

See [HUD.md](HUD.md).

## 9. Numeric data bindings

External mods may register numeric values for JSON-authored interfaces through `NumericDataRegistry`:

```java
NumericDataRegistry.register(
    ResourceLocation.fromNamespaceAndPath("examplemod", "player.mana"),
    context -> getClientManaValue(context.minecraft().player)
);
```

Register during client setup before the HUD is created. The provider is a `NumericDataRegistry.Provider` and receives a `WidgetContext` at evaluation time. UI authors then use `{"bind": "examplemod:player.mana", "fallback": 0}` as a `value` or `max` in an `anchorui:progress` widget. Invalid or missing provider IDs reject the individual widget during creation; a non-finite runtime value or provider exception produces its declared fallback instead.

The numeric registry is intentionally read-only from the JSON side. Client values are for display; any actions affecting server state must be implemented using a server-authorized integration. The initial implementation reads providers as part of widget rendering; no per-tick scanning or expensive JSON parsing is used.

For normal `Screen` interfaces, a mod can also register custom action factories through `UiActionRegistry.register`. The bundled `anchorui:button` compiles its `onClick` object when the screen opens. Only explicitly registered actions can execute, with built-in support currently limited to `anchorui:open_screen` and `anchorui:close_screen`. No arbitrary shell commands or Java execution are enabled through JSON.
