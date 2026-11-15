# Cape Menu Organizer design

Approved scope, updated to the user's latest requests on2026-11-15.

## Purpose

Use one editable layout for the regular max cape in inventory and equipment. The player may mix teleport destinations, individual native Spellbook/Features actions and whole native Spellbook/Features submenus, duplicate them and color each occurrence independently.

## Layout and defaults

Ordered root entries are either custom groups or native action/submenu references. Custom groups contain references only; custom groups cannot nest. Native submenu references can be placed inside a custom group and retain their own native children.

The default layout contains exactly these groups and order:

- Poh: Aldarin, Brimhaven, Hosidius, Pollnivneach, Prifddinas, Rellekka, Rimmington, Taverley, Yanille.
- Guild: Crafting Guild, Farming Guild, Fishing Guild, Hunter Guild, Warrior's Guild.
- Skill: Black chinchompas, Boat, Feldip Hills, Last Boat, Otto's Grotto, The Pandemonium.

No automatic Other teleports menu or reserved group name. Unassigned entries are omitted from the custom layout; the player can add them or show their original menu. Existing layouts are preserved. Reset applies the new default only after confirmation.

## Editor

RuneLite sidebar: catalog of observed native actions/submenus, ordered tree, Add action/group, Rename, Remove, Move up/down/to, Color and Reset color controls. Each occurrence owns its own identity and optional RGB color. Removing one duplicate never removes another. Changes save automatically in the active RuneLite configuration profile.

The picker is seeded with known cape references and saved layout references immediately at startup; it must not be blank until a right-click. Non-mutating hover discovery learns additional current native options. Seeded choices never carry executable action data and do not bypass native availability checks. Use RuneLite's own ColorPickerManager with RGB-only text colors; save on close through the editor session captured at opening, and close owned dialogs on shutdown without saving stale changes.

Original teleport menus have per-context visibility switches. Spellbook/Features originals and individual children have shared visibility switches. Original visibility does not disable independent custom shortcuts. Whole native submenu references respect selected child visibility. Empty custom groups/native submenu references are omitted. The active native default entry and its complete submenu are retained regardless of masking to preserve existing click choices. Shift-right-click swap editing is left intact.

Omit a custom root shortcut when it duplicates the current native default leaf action on the same cape. Preserve the native row, saved occurrence/color, and all deliberately grouped duplicates. The shortcut returns if the active default changes. Match native metadata/callback/context; known direct-versus-submenu Home and Crafting Guild operations can differ in identifier while representing the same destination. A matching name from another item or a different callback must not trigger suppression.

## Native action resolution

Read only current native max cape inventory/worn widgets and menus through public APIs. Never store executable action metadata or synthesize server actions. Copy current leaf identifiers, item/widget/slot/world parameters, flags and callbacks. A copied native submenu is a presentation container with cloned current children; selecting a leaf performs only that native action.

Qualify individual utility references (Spellbook / Ancient, Features / Stamina Boost) to prevent name collisions. Canonicalize Carnivorous chinchompas to Feldip Hills and Warriors' Guild to Warrior's Guild across contexts, including saved references, while preserving occurrence IDs/colors. Resolve action metadata independently from each currently open native menu.

Validate config and menu mapping before mutation. Count actual descendant entries for repeated native submenu references against a conservative450entry budget. Preserve native menus on unrecognized or oversized mappings. Unrecognized utility children stay intact in original menus; whole references are offered only when all children are safely recognized.

## Boundaries

Regular max cape only. No diary cape, combat cape combinations, arbitrary item support, automatic conditional swaps, input injection, synthetic action dispatch, external services or telemetry. Utility actions were explicitly added to scope after the initial teleport-only design. Static user-selected visibility changes do not affect other items or actors.

## Verification

Unit tests cover occurrence independence, defaults, config compatibility/migration, colors, native metadata, inventory/worn context detection, unified utility shortcuts/submenu references, original masks, no fallback menu, profile/lifecycle state and capacity limits. Render sidebar offscreen at225px. Review code without interacting with RuneScape. The user verifies all real actions and destination correctness in both contexts; a build or startup is not an in-game pass.
