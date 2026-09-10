# История изменений

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/), проект придерживается [семантического версионирования](https://semver.org/lang/ru/).

## [4.0.0] — невыпущенная

### Добавлено

- **Правила по мирам и мобам.** `settings.worlds.<мир>` и `settings.entities.<моб>` переопределяют `egg-break-on-spawner`, `egg-break-chance`, `ground-spawn-chance` и `affect-creative`. Приоритет: мир + моб → мир → моб → глобальные значения.
- **Белый список мобов.** `settings.entity-filter: blacklist | whitelist` и `settings.allowed-entities` — режим «в спавнеры можно ставить только этих мобов»; в режиме `whitelist` без `allowed-entities` используется `black-entities`.
- **Плейсхолдеры в сообщениях** (`Placeholders`): `{player}`, `{world}`, `{entity}`, `{chance}`, `{hand}`, `{mode}`; в `/info` и `/stats` дополнительно `{version}`, `{spawner}`, `{break}`, `{ground}`, `{creative}`, `{filter}`, `{cooldown}`, `{used}`, `{broken}`, `{failed}`, `{denied}`, `{succeeded}`.
- **Выбор канала вывода сообщения**: `messages.<ключ>.output: chat | actionbar | title | none`.
- **Настройки звука**: `sounds.<ключ>` теперь принимает секцию с `key`, `volume` (0–10), `pitch` (0–2) и `source` (`master`, `music`, `record`, `weather`, `block`, `hostile`, `neutral`, `player`, `ambient`, `voice`). Строковая форма сохранена.
- **Частицы**: `particles.<ключ>` с `name`, `count`, `spread`, `speed`. Имя разрешается через реестр частиц сервера, поэтому работают и частицы из датапаков; кэш имён сбрасывается при reload.
- **Статистика** (`Stats`): счётчики использованных, сломанных, отклонённых яиц, неудач и успешных призывов с разбивкой по эффектам. Хранится в `stats.yml`, пишется при остановке сервера.
- **Команды** `/smashegg info [мир|моб [моб]]`, `/smashegg stats`, `/smashegg stats reset`; алиас `/segg`; автодополнение подкоманд по правам и имён миров для `info`.
- **Права** `smashegg.info`, `smashegg.stats`, `smashegg.stats.reset` (по умолчанию — операторы).
- **Локализация**: `lang/ru_RU.yml` и `lang/en_US.yml`, выбор через `settings.language`, откат на встроенный русский текст при отсутствии файла или ключа.
- **Настройка поведения при неудаче**: `settings.failure-action: CONSUME | DROP | NOTHING`.
- **Настраиваемый кулдаун** между обработками яиц игрока: `settings.cooldown-ticks` (0 отключает блокировку).
- **Журнал событий** в консоль: `settings.log-events`.
- **Предупреждения о неизвестных полях конфига** (`SectionFields`, `ConfigNodes`) — опечатка в `settings`, `settings.worlds`, `settings.entities`, `sounds`, `particles`, `messages` больше не проходит молча.
- **`LICENSE` (MIT)** и блоки `licenses`, `scm`, `name`, `url`, `description` в `pom.xml`.
- `CHANGELOG.md` и обновление `README.md`, `docs/ROADMAP.md`, `docs/TESTING.md` под новую функциональность.
- Новые тесты: `PlaceholdersTest`, расширены `PluginSettingsTest`, `SoundsTest`, `EggListenerTest`, `CommandHandlerTest`, `StatsTest`, `ColorUtilTest`, `ReloadTest`.

### Изменено

- Звук `sounds.failure` переименован в `sounds.ground-failure` — старое имя продолжает приниматься с предупреждением в консоли.
- `messages.<ключ>` из строки стал секцией `text` / `enabled` / `output`; строковая форма по-прежнему принимается.
- `SmashEgg` отдаёт эффекты через единый путь (`effect`, `message`, `logEvent`) вместо отдельных методов для звука и текста; статистика ведётся в момент срабатывания эффекта.
- `PluginSettings` — правила собираются в `Rules` с переопределениями `Rules.Overrides`, а не набором отдельных полей.
- `ColorUtil` экранирует MiniMessage-теги в подставляемых значениях: имя игрока или мира с `<...>` не разбирается как разметка.
- Версия схемы конфига поднята до `3`; файл с более старой версией загружается с предупреждением.

### Исправлено

- Числовые значения по умолчанию для `volume`/`pitch` больше не отклоняются как «не число»: разбор принимает любой числовой тип YAML, а не только `Integer` и `Double`.

### Совместимость

- Требуется **Paper 1.21+** и **Java 21+**; `api-version: '1.21'`, Adventure 4.17.0 из сервера.
- Конфиги 2.x и 3.x загружаются без правок; удалений ключей нет.
- **Folia не поддерживается** (`folia-supported: false`).

## [3.0.0]

### Изменено

- Переход со Spigot на **Paper API** (`io.papermc.paper:paper-api`), Adventure берётся из сервера; убраны `adventure-platform-bukkit`, shade и relocation.
- Звуки переведены на namespaced-ключи (`entity.player.levelup` вместо `ENTITY_PLAYER_LEVELUP`): в Paper `org.bukkit.Sound` — интерфейс реестра, `Sound.valueOf` не существует. Четыре стандартных имени из 2.x принимаются с предупреждением.

### Добавлено

- `.gitignore`, `.mvn/wrapper/maven-wrapper.properties` (без него `./mvnw` не запускался) и `.github/workflows/build.yml` — сборка и тесты на Java 21 и 25 с публикацией JAR и отчётов.
- Документация: `docs/ROADMAP.md`, `docs/TESTING.md`.

## [2.x]

Исходное состояние плагина: вероятность поломки яиц призыва на спавнерах и на земле, чёрный список мобов для спавнеров, поддержка обеих рук, исключение Creative, право `smashegg.bypass`, MiniMessage-сообщения, безопасный reload. Звуки задавались именами перечисления Bukkit, сборка шла против Spigot API.

[4.0.0]: https://github.com/qweyns/SmashEgg/compare/v3.0.0...HEAD
[3.0.0]: https://github.com/qweyns/SmashEgg/releases/tag/v3.0.0
