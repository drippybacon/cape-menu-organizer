# Cape Menu Organizer

Create a shared custom max cape teleport menu for inventory and equipment.

## Features

- Create and rename your own teleport groups.
- Place shortcuts directly in the main right-click menu or in one-level submenus.
- Add the same destination multiple times, including to different groups.
- Reorder, move and remove individual shortcuts without affecting other copies.
- Choose an optional color for each group or shortcut; duplicate destinations can have different colors.
- Show or hide each original teleport submenu independently for inventory and equipment.
- Selectively hide individual Spellbook or Features options, or either whole submenu, in both contexts.
- Keep unassigned destinations accessible under **Other teleports**.
- Preserve existing left-click and Shift-click choices; other cape actions stay visible unless you choose to hide them.
- Save the layout in the active RuneLite configuration profile.

Version one supports the regular max cape and its worn item representation. Combat max cape combinations and the achievement diary cape are not supported.

## Using the editor

1. Enable **Cape Menu Organizer** and open its cape icon in the RuneLite sidebar.
2. Right-click the max cape in your inventory, then while worn. This loads the destinations exposed by the current game menus. The editor remembers observed names for this session; it never saves executable game actions.
3. Create a group with **Add group**, or select **Main menu** for a direct shortcut.
4. Choose a destination and click **Add teleport**. Repeat to create additional copies.
5. Select an entry and use **Move up**, **Move down**, **Move to…**, **Rename** or **Remove**.
   Use **Color…** to choose that entry's text color, or **Reset color** to restore the default.
6. Tick the original submenu visibility boxes to retain any native teleport groups you want alongside your layout. They start hidden.
7. Under **Other cape actions**, untick **Show Spellbook** or **Show Features** to hide a whole menu, or untick individual options to hide just those entries. Open the cape menu first to populate the available individual options. These settings apply to inventory and worn cape menus and start with all actions visible.

Changes save automatically and take effect on the next menu opening. Empty groups and unavailable destinations do not appear in the game menu. **Other teleports** contains currently available destinations that have no custom shortcut. The native active default entry stays at the top; your custom entries appear immediately below it. This can retain an original submenu if that submenu is itself your active default action.

Hold Shift and right-click to configure swaps through core **Menu Entry Swapper**. The plugin intentionally leaves that configuration menu intact. Release Shift to inspect the organized menu. The plugin does not edit another plugin's configuration.

Reset requires confirmation. If the saved layout is invalid, editing and menu replacement are disabled until you explicitly reset; the original stored value is not automatically overwritten. Disabling the plugin restores normal menus on their next opening.

## Development

The project uses the official RuneLite plugin template, Java 11 bytecode and the template's pinned Gradle wrapper. Use a JDK supported by Gradle 8.10 (the development build was run with Temurin 17).

```powershell
.\gradlew.bat test build
.\gradlew.bat run
```

For a Jagex Account, follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions for signing into a development client. This plugin is a local development project; it has not been published to the Plugin Hub.

## In-game acceptance checks

A passing build verifies local logic, not actual game teleports. The player must verify:

- Open the cape's inventory and worn menus and compare the custom group names and destination order.
- Add Farming Guild to two groups and a direct shortcut. Test each copy in both contexts; remove one and confirm the others remain.
- Test every available destination in each context. Check that the destination reached matches the label and normal restrictions/confirmation prompts remain in effect.
- Toggle each original submenu independently and confirm custom shortcuts and Other teleports remain accessible.
- Give two copies of a destination different colors, color a group, then restart and confirm both menu contexts retain those colors. Reset one color and confirm the other copy is unchanged.
- Hide one Spellbook option and one Features option, then verify remaining options still work in both contexts. Hide every child and confirm the empty parent disappears; re-enable an entry from the sidebar to restore it. Hiding an entire group must retain individual choices for when it is shown again. The active default submenu remains visible to preserve click settings.
- Confirm existing left-click/Shift-click selections, core swap configuration, Wear/Remove, Use/Drop/Examine, Features and Spellbook actions.
- Move the cape to another inventory slot; open unrelated item and bank menus; confirm those menus are unchanged.
- Switch RuneLite configuration profiles and restart the plugin/client to confirm layout persistence and separation.
- Reset and disable the plugin, then reopen menus and confirm the expected defaults/native menus.

If the editor says it could not recognize a native action, the original menu is retained. Report the context and visible native menu options; do not work around it by assuming a teleport action number.
