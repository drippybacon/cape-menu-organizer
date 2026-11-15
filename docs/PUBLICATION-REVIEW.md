# Release review

Reviewed 15 November 2026 for version 3.2.1, against RuneLite 1.13.6 and the current published rules.

| Ready | Needs attention |
| --- | --- |
| Production compilation passes without warnings; 49 automated tests pass. | Player confirmation of the [manual release checks](TESTING.md). |
| Java 11 bytecode, public RuneLite APIs, no additional production dependencies. | Plugin Hub CI and maintainer approval after submission. |
| BSD license, current metadata, concise docs and a valid 15 × 29 PNG listing icon. | |
| Native action metadata/defaults and callbacks preserved; menu limits checked before changes. | |
| Config validation, legacy import, independent layouts/copies and stale-editor protections reviewed. | |

The default layout was compared with the current [Max cape teleport table](https://oldschool.runescape.wiki/w/Max_cape#Teleport_options): all 21 player destinations are included, with Home first in Poh. The wiki’s Wilderness Hunter area and Feldip Hunter area correspond to the native Black chinchompas and Feldip Hills labels. Teleport Boat (Pandemonium) moves the boat rather than the player and remains available as a Features action.

## Findings fixed

- Groups exceeding the native 64-entry submenu limit now keep the original menu before any changes. Tests cover both 64 and 65 actions.
- Dialog results are rejected if settings reload while the dialog is open. Reset and copy retain their original save session.
- Long diagnostics resize correctly instead of clipping.

An independent code review rechecked these fixes and found no remaining important issues. Tests use simulated menus; game actions and visual behaviour still require the player's checks.

## Rules and packaging

[Jagex's guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) explicitly exempt Max cape and achievement diary cape menu additions from the general restriction. The [wiki mirror](https://oldschool.runescape.wiki/w/Update:Third_Party_Client_Guidelines) was fetched in full for this review.

This plugin changes only regular Max cape menus. It copies currently available native actions and callbacks; it does not dispatch synthetic actions, inject game input, resize game click areas, or change other plugins' settings. Visibility choices are static user settings. Its sidebar uses RuneLite's normal navigation API.

The full [RuneLite rejection rules](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features) and [template guidelines](https://github.com/runelite/example-plugin/blob/master/AGENTS.md) were checked. No forbidden-code matches, unused production imports, blocking I/O or template classes were found in production code. The development launcher has the template's generic-varargs warning; it is not shipped in the plugin JAR.

`build=standard` is appropriate: the Hub replaces local build scripts and compiles production sources using its standard dependencies. The inspected local JAR contains only plugin classes and targets Java 11. The listing icon is attributed in [third-party notices](../THIRD-PARTY-NOTICES.md).

## Submission

Push the source to the public [repository](https://github.com/drippybacon/cape-menu-organizer), then use the exact reviewed commit in `plugins/cape-menu-organizer` in a Plugin Hub pull request:

```properties
repository=https://github.com/drippybacon/cape-menu-organizer.git
commit=<full reviewed commit SHA>
```

Follow the [Plugin Hub submission instructions](https://github.com/runelite/plugin-hub#submitting-a-plugin). This review prepares the source for submission; it is not RuneLite approval or a published Hub release.
