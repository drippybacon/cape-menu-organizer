# Testing

Run `.\gradlew.bat test build` from the repository root.

The automated suite covers layout editing/validation, legacy import, independent Normal/Shift saves and copies, stale profile/dialog edits, native action metadata/callbacks, defaults, utility visibility, duplicate suppression and menu capacity. It uses simulated menus; it does not perform game actions.

## Manual release checks

Use the regular Max cape and repeat menu checks in inventory and while worn.

- Give Normal and Shift different groups/order/colours. Check each right-click menu uses the correct layout, regardless of the selected editor tab.
- Test a POH, guild and skilling teleport, including Feldip Hills and boat options. Check normal restrictions and confirmations still apply.
- Test individual Features/Spellbook shortcuts and complete native submenus, both at the root and inside a group. Check hidden originals do not disable direct shortcuts.
- Keep Home as left-click and Crafting Guild as Shift-left-click. Check both still work, main-menu duplicates disappear, grouped copies remain and RuneLite swap controls work.
- Copy in both directions, cancel a copy, then edit only one tab. Check colours/visibility transfer without linking the layouts. Reset one tab and restart; the other must stay unchanged.
- Hide and show the sidebar in plugin settings. Check the Max cape icon returns and custom menus remain active while hidden.
- Switch profiles or reload settings while a dialog/colour picker is open. Old edits must not change the new layout/profile.
- Check long diagnostics are readable. A group with more than 64 available actions must retain the native menu without partial changes.
- Check unrelated items, bank menus, cape slot changes, plugin disable/re-enable and profile/restart persistence.

The automated tests and a successful client start do not replace these game checks.
