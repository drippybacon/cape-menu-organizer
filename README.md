# Cape Menu Organizer

Create one shared max cape menu layout for inventory and equipment.

## Features

- Create, rename and reorder your own groups.
- Mix teleports, individual Spellbook/Features actions, and complete native Spellbook/Features submenus in the same layout.
- Place an action directly in the main menu or inside a custom group.
- Add independent copies of any action or native submenu, with separate colors for each occurrence.
- Show or hide original teleport menus, Spellbook/Features menus, or individual original utility entries.
- Preserve existing left-click and Shift-click choices and normal Wear/Remove, Use/Drop and Examine actions.
- Save the layout in the active RuneLite configuration profile.

Version one of the supported item set includes the regular max cape and its worn representation. Combat cape combinations and achievement diary cape are not supported.

## Default layout

| Group | Actions, in display order |
| --- | --- |
| Poh | Aldarin, Brimhaven, Hosidius, Pollnivneach, Prifddinas, Rellekka, Rimmington, Taverley, Yanille |
| Guild | Crafting Guild, Farming Guild, Fishing Guild, Hunter Guild, Warrior's Guild |
| Skill | Black chinchompas, Boat, Feldip Hills, Last Boat, Otto's Grotto, The Pandemonium |

These defaults apply to new profiles and confirmed resets. Existing saved layouts are preserved. Old Carnivorous chinchompas references resolve to Feldip Hills, retaining occurrence IDs and colors; the action is taken from the currently open native cape menu. The Warriors' Guild spelling is normalized to Warrior's Guild.

There is no automatic Other teleports menu. Only actions assigned to your layout are added. If you want another group, create it and add the actions you want. Original menus can also be shown separately.

## Using the editor

1. Enable Cape Menu Organizer and open its cape icon in the RuneLite sidebar.
2. The action picker is populated immediately with known cape actions and your saved references. Hovering the cape automatically learns additional current options; right-clicking also refreshes them. Editor choices do not invent game actions: custom shortcuts appear only when the corresponding action exists in the current native cape menu. Executable action data is never saved.
3. Select Main menu for a direct shortcut, or create/select a custom group.
4. Choose a teleport, a qualified action such as Spellbook / Ancient, or a complete Spellbook/Features submenu, then click Add action.
5. Repeat to create independent copies. Move up/down controls order entries; Move to moves shortcuts between groups and the main menu. Rename changes custom group names. Remove deletes only the selected occurrence (or a selected group and its shortcuts).
6. Use Color to open RuneLite's normal color picker for the selected group or shortcut. Close the picker to save the selected text color, or use Reset color to restore the default.
7. Original teleport submenus controls show or hide native teleport menus independently in inventory/equipment. They start hidden.
8. Original cape action menus controls show or hide native Spellbook/Features menus and individual entries. They start visible. Hiding a native original does not disable a separately added custom shortcut. A complete submenu reference respects the selected child visibility; direct shortcuts remain independent.

Custom groups cannot contain other custom groups. A complete native Spellbook or Features submenu may be placed inside a custom group, creating an additional native submenu level. Empty/unavailable custom groups and references do not appear in the game menu.

Your active native default entry stays at the top; custom entries follow in layout order. The active default submenu is retained intact even if its visibility box is off, to protect the current click setting. Hold Shift and right-click to configure clicks through core Menu Entry Swapper, then release Shift to inspect your organized menu.

If a custom top-level shortcut duplicates the active native default action, the custom row is omitted for that menu opening. The saved shortcut and color remain unchanged, so it returns when another action is the default. Intentional copies inside custom groups are preserved. This avoids two Home rows with native left-click Home, without changing the native click action.

Changes save automatically and apply on the next menu opening. Reset requires confirmation. Invalid saved data disables editing and menu replacement until you explicitly reset; it is not silently overwritten. Disabling the plugin restores normal menus on the next opening.

## Development

Use the official Gradle wrapper with a supported JDK (development uses Temurin17, Java11 bytecode):

```powershell
.\gradlew.bat test build
.\gradlew.bat run
```

The local Orb Only development launcher also loads Infobox Utilities and this plugin. Rebuild this project's JAR and launch from Orb Only to test all three together. See the sibling Orb Only development documentation for its current file dependency version.

For Jagex Account login, follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions. This is an unpublished local development project.

## In-game acceptance checks

A passing build verifies local logic, not actual game actions. The player must check:

- Confirmed reset/new profile yields the Poh, Guild and Skill defaults above, and existing edited profiles remain unchanged.
- Immediately after plugin/client startup, the action picker contains known choices without first opening a cape menu. Hovering the cape refreshes extra options without changing its current native click behavior.
- The Color button opens RuneLite's standard color picker; closing saves the selected entry's RGB text color. A profile/layout change while it is open must not save through a different editor session.
- Inventory and worn cape custom groups have matching order; no automatic Other teleports group appears.
- Add Spellbook and Features submenus inside a custom group, plus Ancient and Stamina Boost as individual shortcuts alongside a teleport. Test each in both contexts.
- Hide original utility menus/entries and verify independent custom shortcuts remain usable. Whole submenu references should respect child visibility.
- Test Feldip Hills in both contexts and confirm it reaches the intended location. An existing Carnivorous chinchompas shortcut should now show Feldip Hills.
- Test every assigned action, normal restrictions/confirmations, independent duplicate colors, removal/reordering, and persistence across restart/profile changes.
- With native Home left-click and a custom root Home shortcut, confirm only native Home appears at the top. Grouped Home copies must remain. With another native default, the custom root Home must return; duplicate root Crafting Guild should similarly be omitted when it is the native default.
- Confirm existing clicks and Shift-right-click configuration, normal non-teleport item actions, unrelated items/bank menus, cape slot changes, disable/reset.

If native action data is unrecognized, the original menu is retained. A layout that would exceed the menu capacity also falls back before mutation.
