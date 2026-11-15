# Normal and Shift Menus Implementation Plan

> For agentic workers: use superpowers:executing-plans. The user approved the two-tab design and initial copy of the existing layout.

**Goal:** Independently edit and render Normal and Shift layouts, each shared between inventory and equipment.
**Architecture:** Store both layouts atomically in a new versioned `menus` config value. Import the legacy `layout` value into two deep copies when no new value exists, preserving IDs/colors/masks. Select runtime layout from the actual Shift key; UI selection only chooses which layout to edit.
**Tech stack:** Existing Java11/RuneLite/Swing/Gson/JUnit toolchain.
**Spec:** ../specs/2026-11-15-cape-menu-organizer-design.md, Normal/Shift extension.

## Global constraints and review focus
- Preserve existing layout and native click defaults; clone into both modes during migration.
- A save must persist both layouts in one value, without a transient mixed pair or stale profile writes.
- Shift must receive the full custom layout; preserve original RuneLite swap-control entries/callbacks alongside it.
- Each menu has independent groups/colors/masks; reset affects only the selected valid menu.
- Unknown/corrupt combined data must not silently fall back to legacy data; explicit reset restores both when unreadable.
- Color picker callbacks retain their original mode and state session.
- Observe menus without mutation on hover; render only on MenuOpened. No gameplay automation.

## Tasks
- [x] Add MenuLayouts plus codec import/serialization tests: legacy copies are independent, both menus round trip, corrupt combined data is rejected.
- [x] Extend LayoutState snapshot/save with paired menus and mode selection; test saving/resetting one preserves the other and stale saves fail.
- [x] Add Normal/Shift tabs around two existing editors; test edits route to the selected editor's mode and keep colors/masks independent.
- [x] Select runtime layout using KeyCode.KC_SHIFT; remove the old special supplemental-only Shift path and retain core swap entries through full transformation. Update Shift tests to prove groups/masks apply and callbacks/default stay intact.
- [x] Update docs/version/combined launcher, run full tests/build, render both tabs, request code review and save local checkpoint.
- [x] Open updated combined development client for user verification of normal/Shift in inventory/worn and click-default preservation.
