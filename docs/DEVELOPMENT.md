# Development

Use JDK 17 and the included Gradle wrapper. Production code targets Java 11 and uses RuneLite's public API.

## Build and run

From this repository on Windows:

```powershell
.\gradlew.bat test build
.\gradlew.bat run
```

On macOS/Linux, use `./gradlew` instead. For Jagex Accounts, follow RuneLite's [login guide](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

The sibling Orb Only development launcher can load this plugin alongside Infobox Utilities. Rebuild this project's JAR first and update that launcher's local file dependency when the version changes. No sibling repository is needed to build this plugin alone.

## Project notes

- `runelite-plugin.properties` is the source of the version and Hub metadata. `build=standard` uses the Hub's standard build; there are no extra production dependencies.
- `CapeContext` limits changes to the regular Max cape in inventory/equipment.
- `CapeMenuTransformer` copies current native actions and callbacks. It checks root and submenu capacity before changing a menu.
- `MenuLayout`, `LayoutCodec` and `LayoutState` validate and save both layouts together. The old single layout imports into independent Normal/Shift copies; its config key is retained.
- Dialogs and colour pickers reject stale edits after a layout/profile change. Invalid saved data keeps native menus until an explicit reset.

See [testing](TESTING.md) for the release checks. The final game checks must be performed manually by the player.
