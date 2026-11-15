# Cape Menu Organizer design

Status: approved by user on 2026-11-15, including duplicate shortcuts and hideable original teleport submenus.

## Purpose and scope

Make max cape teleports predictable in inventory and equipment by displaying the same user-created menu layout in both places. The user already has left-click and Shift-click configured and wants access to the remaining teleports without learning two layouts.

Version one supports the regular max cape. Achievement diary cape support is deferred until an owner can verify it. Cosmetic max cape variants are included only if they expose the same teleport functionality; combat cape combinations are not assumed to do so.

Proposed display name: Cape Menu Organizer. Intended new repository: `C:/Users/sebas/Documents/git/runelite-plugins/cape-menu-organizer/`. The repository was created from the official template and is being validated locally.

## Approaches

1. Fixed matching categories: small implementation, but cannot express the user's preferred layout.
2. Favorites plus predefined categories: convenient shortcuts, but still constrains organization.
3. User-created groups and direct teleport entries: the user's requested approach. One shared layout controls both item contexts, and a destination may appear multiple times, including in different groups.

The proposed editor is a RuneLite sidebar panel. Editing through the game menu would crowd the menu and complicate managing many entries. Sidebar editing is part of the approved design.

## Player experience

The sidebar contains a destination catalog and an ordered layout editor. The player can add a teleport directly to the main menu, create or rename a group, add destinations to that group, and move or remove entries. Destination entries use canonical destination names so duplicate shortcuts remain recognizable.

Groups contain teleport entries only: one level of submenus in version one. The same destination can appear at the root and in several groups. Removing a shortcut removes only that occurrence. Removing a group removes its shortcuts, never the underlying teleport capability. Empty groups are not displayed in the game menu.

User addition during implementation: each custom group and teleport occurrence has an optional RGB text color. A color picker and reset-color control edit the selected entry. Copies of the same destination can have different colors. Colors affect presentation only; action identity and metadata remain unchanged. Unassigned/native menu entries retain their native presentation.

Use clear Add, Rename, Remove, Move up, and Move down controls for the first version; drag-and-drop is optional later. Group names must be nonempty plain text with a practical length limit and no game formatting tags. Saving edits takes effect when the next menu is opened.

Unassigned destinations remain available under a reserved `Other teleports` submenu. A reset action restores a supplied matching layout after confirmation. Disabling the plugin restores normal menus immediately.

The editor also lets the player show or hide each original teleport submenu independently in each context. Hiding an original submenu only removes its native presentation; its destinations remain available to custom groups and direct shortcuts. The `Other teleports` fallback ensures unassigned destinations remain reachable even when original teleport submenus are hidden. These visibility controls do not hide non-teleport cape menus such as spellbook changes or features. Original teleport submenus start hidden to give a matching custom layout; the native active default entry is always retained, including if it is a submenu.

The custom right-click teleport section has identical names and order in both contexts. Non-teleport actions such as Wear, Remove, Use, Drop and Examine retain their context-appropriate behavior. Existing left-click and Shift-click choices remain functional; this plugin does not change another plugin's settings. An already promoted click destination may also appear in the organized section.

User-requested extension: the sidebar lists observed native Spellbook and Features actions and lets the player hide individual child entries or either entire submenu. These visibility choices are shared across inventory and equipment, start fully visible, and persist without replacing existing layouts. Unrecognized children remain unchanged. Empty parents disappear, except the active native default entry and its submenu are retained. This filtering changes visibility only, never surviving action metadata.

## Implementation boundaries

Use the official RuneLite Java template and public menu APIs. Store the ordered groups and destination references in this plugin's own profile configuration. Give each layout occurrence its own identity, separate from the destination identity, to support duplicates reliably. Include a configuration schema version.

Identify teleports by verified destination identities and context-specific native action data, not menu text alone. Rebuild only the cape's teleport section when the right-click menu opens; preserve the native default-click path. Each shortcut must dispatch exactly the corresponding existing teleport action from the currently hovered item and context. No automatic equipment changes, extra game actions, injected input, or speculative packet/action construction.

Before implementation is considered viable, verify how the current client exposes all max cape destinations in inventory and equipment and whether native action data can be safely retained when entries are copied into submenus. In particular, validate promoting or duplicating nested entries and compatibility with core Menu Entry Swapper. Do not use prohibited action-dispatch APIs as a workaround. If the APIs cannot preserve the required behavior, report that limitation before changing the scope.

Resolve the whole destination mapping before replacing the menu. If a context cannot be safely recognized or mapped, leave its native menu intact and provide a useful diagnostic. Invalid saved configuration must fall back safely without silently overwriting the player's saved layout. Only list destinations whose availability has been verified for the current cape; preserve native restrictions and confirmation flows.

No external service, telemetry, overlay, arbitrary item support, automatic region-dependent swapping, spellbook organization, or nested groups in version one.

## Rules and prior art

Prior checks found core Menu Entry Swapper and Custom Menu Swaps cover click shortcuts and priorities, but no verified existing plugin provides this shared user-authored group layout. The user explicitly requested creating the plugin after that overlap finding.

Jagex's current guideline text contains an exception for additional server-action menu entries for max and achievement diary capes. General menu restrictions still apply. Current official rules take precedence over the template's summarized checklist. Recheck compliance and duplication against the settled scope before scaffolding.

## Verification

Automated tests should cover layout order, duplicate occurrences, independent removal, unassigned destinations, config round trips and invalid config handling. Menu transformation tests should verify equivalent visible teleport structures across inventory and worn contexts, retained action metadata, preservation of non-teleport entries and safe fallback on incomplete mappings.

Also verify independent original-submenu visibility settings in both contexts, custom shortcuts remaining functional when their original submenu is hidden, and continued access to unassigned destinations.

Compile and run relevant tests using the template's supported build. Only the user can verify gameplay: inspect both menu layouts, test duplicate shortcuts and every destination in both contexts, confirm existing left/Shift clicks, native restrictions, cape feature menus, and disabling/reset behavior. Do not claim in-game correctness from a successful build.

## Remaining builder decisions

Choose exact sidebar widget arrangement, safe config size/name limits, default matching categories, action identity mappings and menu event ordering through source inspection and focused tests. Avoid premature abstraction for diary cape support; keep layout data independent of max cape action mappings so that adding another cape later remains possible.
