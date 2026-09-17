# Питомец Финни — Android-приложение (Kotlin + Jetpack Compose)

Функциональный прототип по техническому заданию Департамента финансов города Москвы (2026):
игровой сервис для формирования базовых финансовых навыков у детей 7–11 лет.

## Состояние (спринт 1)

- ✅ Скелет Gradle: модули `app`, `core:economy`, `core:data`, `core:design`
- ✅ Игровая экономика (`core:economy`): BudgetEngine, SavingsEngine, RewardEngine, PetEngine — чистый Kotlin, **14 юнит-тестов**
- ✅ Room-схема (`core:data`): профиль, периоды, транзакции, прогресс заданий + DAO + репозиторий
- ✅ Тема и компоненты (`core:design`): шрифт ≥16 sp, элементы ≥48 dp (ТЗ п. 3.6)
- ✅ Экраны: онбординг (3 типа решений), создание питомца (гостевой профиль, 27 комбинаций внешности), главный экран с питомцем/балансом/состоянием
- ✅ Сборка: `app-debug-v0.1.0.apk` (17 МБ, ноль запрошенных разрешений)
- ⏭ Дальше: план бюджета, задания, магазин, накопления, итоги периода, раздел взрослого, демо-режим

## Сборка

Требования: JDK 17+ (встроен в Android Studio), Android SDK 34 (платформа android-34 установлена на машине).

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :core:economy:test      # юнит-тесты экономики
./gradlew :app:assembleRelease    # релиз (потребуется подпись — см. ниже)
```

Важно: проект лежит в папке с кириллицей (`Работа/Финансы`). JVM на Windows читает
@argfile тестовых worker в CP1251, а Gradle пишет его в UTF-8 — из-за этого classpath
тестов ломается. Поэтому build-директории перенаправлены в ASCII-путь
`C:/Users/vovas/.finni_build` (см. `build.gradle.kts`, переопределяется
`-Pfinni.build.root=...`). В репозиторий-сборку для команды рекомендуется клонировать
в ASCII-путь (например, `C:\StudioProjects\Finni`) — тогда обход не нужен.

## Структура

```
app/                    — приложение: MainActivity, навигация, экраны
  src/main/assets/content/ — учебный контент (6 заданий, 8 товаров, 3 цели, словарь)
core/economy/           — чистая игровая логика (без Android-зависимостей)
core/data/              — Room: entities, DAO, БД, ProfileRepository
core/design/            — FinniTheme, типографика, кнопки ≥48 dp
```

## Подпись релиза (для RuStore)

```bash
keytool -genkeypair -v -keystore finni-release.jks -alias finni \
  -keyalg RSA -keysize 2048 -validity 10000
# keystore НЕ коммитить в git; подключить в app/build.gradle.kts (signingConfigs)
```

## Установка на устройство

```bash
adb install -r app-debug-v0.1.0.apk
```
