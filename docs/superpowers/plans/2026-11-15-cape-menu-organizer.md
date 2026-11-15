# Cape Menu Organizer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a shared, editable max cape teleport menu with duplicate shortcuts and optional original submenus.

**Architecture:** Transform the right-click menu only, copying currently available native action metadata into new entries. Keep an ordered, versioned layout separate from the context-specific action catalog. A Swing sidebar edits this plugin's own profile configuration.

**Tech Stack:** Official RuneLite Java 11 template, Swing, injected Gson and ConfigManager, JUnit 4.

**Spec:** ../specs/2026-11-15-cape-menu-organizer-design.md

## Global Constraints
- Max cape only for v1; no diary cape or combat cape combinations.
- One level of submenus; destinations may occur multiple times.
- Preserve existing default and Shift clicks, non-teleport actions and native restrictions.
- Never call Client.menuAction or synthesize input; copy native actions from the current menu.
- No extra development tools installed. New repository is separate from existing plugins.
- User approved design and instructed execution; implement inline without another approval round.

## Review Focus
- Native actions change after a game update: preserve original menus on unrecognized action data.
- Profile switching or malformed config: reload correctly and never overwrite invalid saved data implicitly.
- Multiple cape items or bank menus: avoid transforming another item's actions or bank interactions.
- Existing click swaps and Shift configuration menus: do not replace the active default or the swap editor.
- Repeated menu opens: use fresh action references, avoid duplicating plugin entries.

### Task 1: Official project and persistent layout
**Files:** template build/manifest; `src/main/java/com/capemenuorganizer/MenuLayout.java`, `LayoutCodec.java`, `CapeMenuOrganizerConfig.java`; `src/test/java/com/capemenuorganizer/MenuLayoutTest.java`, `LayoutCodecTest.java`.
**Interfaces:** `MenuLayout` holds ordered root entries (group or destination), occurrence IDs and per-context hidden original groups. `LayoutCodec.decode(String)` validates version, sizes, IDs, names and structure; `encode(MenuLayout)` round trips. Deep copy isolates client and Swing state.
- [ ] Clone official template, read its AGENTS.md, rename and compile baseline; initialize feature branch and commit scaffold with known author identity.
- [ ] Write failing tests for duplicate shortcuts, independent removal/reordering, no nested groups, plain group names, config round trip, malformed/versioned config and size limits.
- [ ] Run tests, implement model/codec to satisfy them, rerun full suite and commit.

### Task 2: Native cape menu organization
**Files:** `CapeMenuTransformer.java`, `CapeMenuOrganizerPlugin.java`; `CapeMenuTransformerTest.java`, test-only menu fixtures.
**Interfaces:** `CapeMenuTransformer.transform(Menu, MenuLayout)` returns a result with current catalog and status. Operates only on verified max cape inventory or worn item widgets; preserves all native action fields and callbacks. Plugin handles MenuOpened after core handlers and profile/config events.
- [ ] Inspect current menu APIs and source for copying native actions and context identification.
- [ ] Write failing menu-fixture tests for same custom layout across inventory/equipment, duplicate destinations, metadata/callback retention, unassigned fallback, original submenu visibility, preservation of active default/non-teleport actions, unrelated/bank menus and unknown mappings.
- [ ] Implement snapshot/validation before mutation; resolve destinations through verified native teleport branches of the current cape. Learn canonical catalog from observed native entries; never persist executable action metadata. Report available destinations in the editor after opening a cape menu.
- [ ] Run full suite and commit.

### Task 3: Sidebar editor and lifecycle
**Files:** `CapeMenuPanel.java`, plugin integration, README and bundled sidebar icon; sidebar/model tests where meaningful.
**Interfaces:** Panel consumes a copied layout plus destination catalog and emits validated changes through a save callback. ConfigManager owns profile persistence. EDT owns Swing; client thread owns menu reads/writes. Layout snapshots cross threads safely.
- [ ] Build tree editor with destination picker, Add group, Add teleport, Rename, Remove, Move up/down and Move to group/root controls; duplicates get distinct occurrence IDs.
- [ ] Expose per-context original-submenu visibility toggles, Other teleports explanation, status and confirmed reset. Hide original teleport submenus by default for a matching layout; retain native default clicks.
- [ ] Handle profile reloads, invalid config without implicit overwrite, lifecycle cleanup and reset. Use a small generated vector-drawn bitmap icon without external dependencies.
- [ ] Verify editor behavior with headless Swing tests where possible; run full suite and build.
- [ ] Document user testing and development launch instructions; commit.

### Task 4: Review and handoff
- [ ] Request fresh whole-project code review; address important issues with regression tests.
- [ ] Run full tests, build, template-leftover scan and diff checks; document observed results.
- [ ] Keep local repository on its feature branch; do not publish. Offer development client launch and ask user to test all destinations in both contexts, duplicate shortcuts, original-group visibility and existing click swaps.
