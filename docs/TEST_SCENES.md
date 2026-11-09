# Тестовые сцены Anchor UI

Тестовая среда работает на клиенте NeoForge 1.21.1 и использует основной путь от JSON до интерфейса: `LoreEntity → ItemLoreScreen → AnchorLayout`.

## Запуск

1. Запусти Minecraft с модом Anchor UI. Для разработки можно выполнить `./gradlew runClient`.
2. Войди в одиночный мир или подключись к серверу. Для команды не требуются права оператора или установка Anchor UI на сервер.
3. В чате введи `/anchorui test list` для списка сцен либо `/anchorui test scene1` для запуска нужной сцены.
4. Кнопки со стрелками переключают сцены. **G** включает и отключает контуры, ID элементов и центральные направляющие. **Esc** закрывает экран.
5. В `scene5` измени размер окна или значение «Масштаб интерфейса» в настройках Minecraft.

JSON-файлы находятся в `src/main/resources/assets/anchorui/anchorui/test/scenes/`. После изменений ресурсов используй **F3+T**.

## Проверки

| Сцена | Возможность | Ожидаемый результат |
| --- | --- | --- |
| `scene1` | Девять точек привязки | Девять прямоугольников находятся у соответствующих углов, краёв и в центре экрана |
| `scene2` | Относительное позиционирование и цепочки зависимостей | Дочерние элементы располагаются вокруг `panel`, а `grandchild` следует за `right`, несмотря на более раннее объявление в JSON |
| `scene3` | Размеры, отступы, смещения, цвет и прозрачность | У элементов различаются размеры и отступы. Три образца одного цвета должны различаться прозрачностью: 27%, 53% и 100% |
| `scene4` | Модель предмета и форматированный текст | Предмет отображается статично и по центру своей области. Текст переносится; жирность, курсив, подчёркивание и подсказки работают |
| `scene5` | Пересчёт позиции после изменения размеров экрана | Элементы у краёв следуют за краями экрана, связанные с центром — за центральным элементом |
| `scene6` | Скрытый элемент как якорь | Скрытая красная рамка не отображается в обычном режиме, но видимые зависимые элементы располагаются относительно неё |
| `scene7` | Ссылка на отсутствующий элемент | Элемент отображается относительно `screen`, а в консоль выводится предупреждение `WARN` с исходным ID `missing_target`; вылета нет |
| `scene8` | Циклическая привязка | Появляется сообщение `Circular frame anchors`; вылета нет |
| `scene9` | Ванильный `StringWidget` | Текст выравнивается слева, по центру и справа; вертикально центрируется; длинная строка сокращается многоточием внутри прямоугольника |
| `scene10` | Разметка и многострочность | Форматирование через теги, подсказки при наведении, автоматический перенос по словам и явные переводы строки |
| `scene11` | Регистрация пользовательского типа | Два счётчика `anchorui_test:counter` реагируют на клики независимо; второй счётчик привязан к первому |
| `scene12` | Изоляция ошибок фабрик | Ненайденный тип и ошибка фабрики дают предупреждения, а остальные текстовые виджеты продолжают отображаться |
| `scene13` | Типизированный `record` и `Codec` | Кнопки со значениями по умолчанию и заданными полями работают; неверный тип и `step: 0` пропускаются с `WARN` |
| `scene14` | Импорт и namespaced widget IDs | Заголовок из `anchorui_test`, локальный текст, зависимый от него, и импортированный счётчик располагаются правильно |

### `scene2`: что проверяет grandchild

`grandchild` ссылается на `right`, который, в свою очередь, привязан к `panel`. Его **верхний левый угол** (`point: TOPLEFT`) привязан к **нижнему левому углу** `right` (`anchor: BOTTOMLEFT`) со смещением `x: 0, y: 7`.

Таким образом, `grandchild` расположен строго под `right`, с зазором **7 GUI-пикселей**. В файле `grandchild` записан раньше `right`; порядок описания не должен влиять на результат.

### `scene3`: как работает offset и прозрачность

`offset` использует `relativeTo: "primary"`. Его нижняя точка `BOTTOM` привязана к верхней точке `TOP` элемента `primary`, а `y: -14` сдвигает его ещё на 14 пикселей вверх. `x: 0` означает совпадение горизонтальных центров.

`alpha27`, `alpha53`, `alpha100` имеют **одинаковые RGB-компоненты** `39C8AF`, но разные альфа-каналы (`44`, `88`, `FF` в шестнадцатеричной записи). Под ними должен просвечивать фон с разной интенсивностью. Изменение цвета текста проверяется отдельно.

### `scene4`: модель предмета

Предмет отображается в центре собственного прямоугольника и не вращается при перемещении или перетаскивании мыши. Масштаб ограничивается размерами виджета; если предмет выходит за его границы, это ошибка.

### `scene6`: как проверить скрытый якорь

Без контуров (`G` выключен) видны только `note`, `dependent` и `chain`. Красный прямоугольник `invisible` с текстом `YOU MUST NOT SEE THIS` не должен отображаться.

С включёнными контурами (`G`) видна **только диагностическая рамка** `invisible (hidden)` в центре экрана. При этом `note` остаётся выше неё, `dependent` — ниже, а `chain` — ниже `dependent`. Скрытие элемента не удаляет его из расчёта координат.

### `scene7`: неизвестная ссылка

В `scene7` единственный элемент `broken` имеет `relativeTo: "missing_target"`. Такого ID в сцене нет, поэтому компоновщик заменяет цель на `screen` и продолжает расчёт, сохраняя `point`, `anchor`, `x` и `y`. В данной сцене верхний левый угол `broken` оказывается в центре экрана. В консоль Minecraft выводится `WARN` с ID элемента `broken` и отсутствующей цели `missing_target`. В интерфейсе появляется жёлтая диагностическая строка. Ошибка конфигурации не мешает отрисовке остальных элементов.

### `scene8`: циклическая зависимость

В `scene8` элемент `first` привязан к `second`, а `second` — к `first`. Чтобы вычислить координаты любого из них, сначала нужны координаты другого. Компоновщик определяет цикл `first → second → first`, выводит в консоль Minecraft запись `ERROR` с описанием цикла и сообщает об ошибке на экране вместо бесконечной рекурсии.

### `scene9`: отрисовка ванильным StringWidget

Сцена проверяет новую реализацию `AnchoredTextWidget`, где отрисовку текста выполняет `StringWidget` Minecraft. Проверь:

- надписи `LEFT`, `CENTER` и `RIGHT` располагаются соответственно у левого края, по центру и у правого края **внутренней области**, учитывая `padding`;
- все надписи центрированы по вертикали в отведённых им блоках;
- длинный текст в узком блоке `ellipsis` заканчивается многоточием, без выхода текста за границы;
- фон и контуры (G) по-прежнему соответствуют внешнему элементу Anchor UI;
- `scene1`, `scene2` и `scene3` после замены виджета не теряют правильного позиционирования.

В JSON у элемента `type: "text"` можно использовать `"align": "left" | "center" | "right"`. Значение по умолчанию — `left`. При обращении через Java поддерживается `Component`, в том числе переводимые компоненты.

### `scene10`: теги форматирования и многострочный текст

Открой `/anchorui test scene10`. Первый блок проверяет `<b>`, `<i>` и `<u>`. Во втором наведи курсор на подчёркнутые слова внутри `<t id="Подсказка Anchor UI">...`: должна появиться подсказка.

Третий блок проверяет `"wrap": true`, сохранение форматирования при переносах и автоматическую высоту (`"height": 0`). Четвёртый содержит явные переводы строки `\n` и выравнивание по центру.

Для `type: "text"` перенос включается через `"wrap": true` либо автоматически при наличии `\n` в тексте. Без переноса используется `StringWidget`, сохраняющий сокращение длинных строк многоточием. Если указано `"wrap": true`, но ширина равна нулю, применяется ширина 160 GUI-пикселей. При фиксированной небольшой высоте многострочный текст обрезается по внутренним границам элемента.

## Автоматическая проверка

GitHub Actions запускает `gradle build`, включая JUnit-тесты. Проверяется наличие четырнадцати JSON-сцен, разрешение корректных привязок при разных размерах экрана, обработка неизвестных ссылок и циклов. Внешний вид и интерактивность всё равно необходимо проверять в Minecraft.

Собранный JAR должен называться `anchorui-neoforge-1.21.1+<версия_мода>.jar`.

## Пространства имён у типов виджетов

Во всех встроенных тестовых сценах используются полные типы: `anchorui:text`, `anchorui:model` и `anchorui:list`.

Ранее использовавшиеся короткие значения `text`, `model`, `list` сохраняются ради совместимости: они автоматически рассматриваются как `anchorui:text`, `anchorui:model` и `anchorui:list`. Значение `othermod:text` является самостоятельным идентификатором и не создаёт наш текстовый виджет.

Ключи стилевых секций могут иметь полные имена, например `"anchorui:text"`. Для встроенных типов сохраняется поддержка старых секций `"text"` и т. п.; при наличии обоих вариантов предпочтение отдаётся полному. Для чужих неймспейсов короткие ключи не используются.

Пока неизвестный тип пропускается с предупреждением в журнале. Механизм регистрации собственных типов другими модами будет разработан следующим отдельным этапом.

### `scene11`: сторонний виджет

Открой `/anchorui test scene11`. Внутри зарегистрирован `anchorui_test:counter`: фабрика возвращает обычный ванильный `Button`, который Anchor UI размещает и добавляет на экран. Счётчик слева начинается с нуля и увеличивается на 1, справа — с 10 и увеличивается на 5. Нажимай оба по очереди: значения должны меняться независимо. При включённых контурах `G` оба счётчика должны иметь свои рамки и правильные идентификаторы. Положение правой кнопки зависит от `counter_a`.

### `scene12`: неизвестный тип и исключение фабрики

Элемент `unregistered:missing_widget` намеренно отсутствует в реестре; `anchorui_test:broken` зарегистрирован, но бросает исключение из фабрики. Оба должны быть пропущены. На экране останутся `still_alive` и `after_unknown`; они должны сохранять корректное положение и порядок, без падения клиента. В логе появятся `WARN` для отсутствующего типа и `ERROR` со стеком исключения фабрики. Сверху тестового экрана отображается количество предупреждений.

## Разработка собственных виджетов

Публичные API и пример использования для другого мода: [CUSTOM_WIDGETS.md](CUSTOM_WIDGETS.md).

### `scene13`: конфигурация через record и Codec

Открой `/anchorui test scene13`. Виджет `anchorui_test:counter` теперь зарегистрирован с `CounterConfig.CODEC`. Левый счётчик не содержит полей `initial` и `step` — должны примениться значения по умолчанию `0` и `1`. Правый использует `initial: 30` и `step: 3`.

В этом же JSON есть два намеренно ошибочных элемента: строковое значение `step: "invalid"` и `step: 0`. Оба пропускаются, в логах появляются предупреждения с информацией об ошибках декодирования. При этом заголовок, два рабочих счётчика и подпись должны остаться на месте. Проверки `Codec` на значения по умолчанию, чужие поля, ошибочные типы, диапазоны и обратное преобразование выполняются также через JUnit.

### `scene14`: импорт виджетов другого мода

Открой `/anchorui test scene14`. Эта сцена загружает определения из другого пространства имён, `anchorui_test`:

- `assets/anchorui_test/anchorui/widgets/foreign_header.json` экспортирует `anchorui_test:foreign_header`. Экран явно добавляет его через `imports`.
- Собственный элемент `anchorui:explanation` ссылается на `anchorui_test:foreign_header` через `relativeTo`.
- Определение `anchorui_test:foreign_counter` используется через `ref`, но создаётся с локальным ID `anchorui:local_counter` и своими значениями `initial: 12`, `step: 3`.

Проверь, что все четыре элемента располагаются последовательно, кнопка начинается с 12 и увеличивается на 3, а при нажатии G видны полные ID с неймспейсами. Важно: описание чужого виджета загружается в общий каталог, но его экземпляр существует только в том экране, куда он импортирован.

## HUD testing: `/anchorui test hud1`

Test HUDs render directly on top of the Minecraft world and do not open a `Screen`.

1. Join a world and run `/anchorui test hud1 on` (or `/anchorui test hud1` to toggle).
2. Close the chat. At the upper left, a header imported from the `anchorui_test` namespace should appear, followed by an Anchor UI text label, a diamond item, a second namespace label and a copied template `ref` below it.
3. A separate `HUD corner anchor` label should remain 16 GUI pixels from the bottom-right corner when resizing the window or changing GUI Scale.
4. Press **F1**. The Anchor UI HUD elements should disappear with the rest of the Minecraft GUI. Press F1 again to restore them.
5. Open inventory or another game screen; the Anchor UI HUD is intentionally hidden until the screen closes.
6. Use `/anchorui test hud1 off` to disable the preview. Check that these test-only widgets remain hidden after restarting with the preview off.
7. With the HUD visible, press F3+T to reload resources. The renderer should rebuild from the new loaded JSON without multiplying the displayed widgets.

The preview is assembled from `assets/anchorui/anchorui/hud/hud1.json`, `assets/anchorui_test/anchorui/hud/hud1_extra.json`, and the exported `anchorui_test:hud_header` template at `assets/anchorui_test/anchorui/widgets/hud_header.json`.

JUnit checks cross-namespace layout, template overrides and bottom-right anchor positioning at different screen resolutions. A successful build verifies compilation and these data/layout tests; the in-game rendering still needs a manual visual check.

## HUD editor regression

With `/anchorui test hud1 on` active, enter `/anchorui hud edit` or press **F8**. Select the header and move it to verify that `anchorui:hud_body`, `anchorui:hud_item`, `anchorui_test:hud_extra` and the copied template retain their relative anchors. Use the arrow keys, Shift + arrows, **Reset widget** and **Reset all**. **Cancel / Esc** must roll back changes; **Save & close** must persist them.

Saved offsets are written to `config/anchorui/hud-layout.json`. Open the editor a second time and restart the client to verify persistence. When the chat screen is open, the HUD must remain visible; when the inventory is open or F1 hides the GUI, it must disappear. Normal HUD buttons must remain non-interactive. Automated `HudLayoutOffsetsTest` covers JSON persistence, zero-offset removal, invalid input, and parent/child movement across namespaces.

The editor also supports **Point**, **Anchor** and **Change target** to adjust anchoring while preserving screen position; a **Hide / Show** toggle; and **Tab** to temporarily hide toolbars and edit bottom-corner widgets. Hidden widgets remain available as anchor targets. User overrides are stored per namespaced ID, so independent mods can retain independent positions.

### Parent and anchor selection behavior

With HUD Editor open, press **Point** or **Anchor** on a selected widget: its actual on-screen position should move to match the new attachment point. The editor must not compensate the movement by changing manual offsets. Press **Change parent** and then click another HUD widget; the parent should change on that click. Click the same widget, or try to create a parent-child cycle: no change should be accepted. Click blank background to attach to `screen`; **Esc** exits parent-selection mode without closing the editor. The bottom-right `anchorui_test:hud_corner` should be clickable with the top-center control panel visible.

### Compact right-side HUD inspector

Test `/anchorui test hud1 on` after removing saved overrides or using **Reset all**. Header/body, body/diamond, diamond/extra and extra/copied header should touch with no gaps. Open `/anchorui hud edit`: a narrow draggable panel should appear on the right, away from the bottom-right corner widget. Drag its title, select different widgets, then scroll their inspector values with the mouse wheel and scrollbar. Verify namespaced IDs, current position and size, parent and anchor points, visibility, source `position`, `size`, `padding` and custom JSON values. Only the selected or hovered widget's ID label should be drawn in the game area.
