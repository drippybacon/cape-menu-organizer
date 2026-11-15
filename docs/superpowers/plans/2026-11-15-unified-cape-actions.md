# Unified Cape Actions Implementation Plan

> For agentic workers: use superpowers:executing-plans. User directly specified and authorized these changes.

Goal: mix all supported native cape actions/submenus in the editor, remove fallback menu, implement screenshot defaults and resolve Feldip Hills aliases.
Architecture: shared action references map to current-context native snapshots; utility containers contain only safely recognized native leaf snapshots. Published layout state remains profile-isolated.
Spec: ../specs/2026-11-15-cape-menu-organizer-design.md

## Constraints and review focus

- Keep native current-context action metadata and existing click choices.
- Preserve saved custom layouts and occurrence IDs/colors during alias migration.
- Never generate an automatic fallback menu.
- Count actual copied submenu descendants before any mutation.
- No game input automation or invented server action IDs.

## Tasks

- [x] Reproduce name mismatch and missing utility reference behavior in failing tests.
- [x] Implement canonical naming, saved reference migration and exact screenshot defaults.
- [x] Unify utility leaf/container references with teleport actions and remove fallback creation.
- [x] Add capacity regression, observe its failure, then implement descendant-aware preflight.
- [x] Update editor language, documentation, version and combined development launcher.
- [x] Review source, run full builds/tests and inspect a standalone sidebar render.
- [x] Save local checkpoint and launch the combined development client for user verification.

Manual verification remains with the user after launch: reset/default groups, Feldip Hills from inventory/worn, mixed actions and native submenu references, existing clicks, colors and persistence.
