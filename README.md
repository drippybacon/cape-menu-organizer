# Cape Menu Organizer

Organise the Max cape menu with your own groups, order and colours. Use the same layout in your inventory and when worn, with separate menus for normal and Shift right-click.

## Edit your menus

1. Open the **Max cape icon** in the RuneLite sidebar.
2. Choose **Normal** or **Shift**. Holding Shift in game selects the Shift menu; the editor tab only chooses which layout you edit.
3. Select **Main menu** or a group, choose an action, then click **Add action**. **Add group** creates a submenu.
4. Use **Move up/down**, **Move to**, **Rename**, **Remove** and **Color** to arrange it. Actions can appear more than once, with separate colours.
5. Use **Copy to Shift/Normal** to copy the whole layout to the other tab. Confirming replaces that tab; later edits remain independent.

Teleports, individual Features/Spellbook actions and complete Features/Spellbook submenus can be mixed together. Custom groups cannot contain other custom groups.

## Visibility and settings

Under **Show original menus**, checked entries stay visible. Hiding an original menu does not disable your custom shortcuts. A copied Features/Spellbook submenu follows its child visibility settings.

**Show sidebar** in the plugin settings hides the editor button while keeping your custom menus active.

Changes save automatically per RuneLite profile. **Reset layout** restores the selected tab's defaults: Poh (Home first, then all house portals), Guild and Skill groups. Together they include all 21 player teleport destinations.

## Compatibility

Supports the regular Max cape in inventory and equipment. Combat combinations and the achievement diary cape are not supported.

Your existing left-click and Shift-left-click choices stay in place. RuneLite's swap controls remain available with Shift-right-click. A custom main-menu action matching the active default is omitted to avoid a duplicate; copies inside groups remain.

Shortcuts appear only when their native cape action is available. Oversized or unrecognised menus keep the original menu. Layouts support 100 entries in total; each displayed submenu supports 64.

## More information

[Development](docs/DEVELOPMENT.md) · [Testing](docs/TESTING.md) · [Release review](docs/PUBLICATION-REVIEW.md)

BSD 2-Clause. See [LICENSE](LICENSE) and [third-party notices](THIRD-PARTY-NOTICES.md).
