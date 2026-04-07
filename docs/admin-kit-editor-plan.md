# Admin In-Game Kit Editor (Drag-and-Drop) – Implementation Plan

## 1) Goals
- Let admins create and edit kits fully in-game via an inventory GUI (no manual YAML editing required for common workflows).
- Support drag-and-drop item composition for kits, including vanilla items and provider-backed custom items (Nexo + ExecutableItems).
- Preserve existing claim/preview behavior for players while introducing a separate admin workflow.
- Keep compatibility with existing kit files and migrate incrementally.

## 2) Current State (Baseline)
- Kits are loaded from YAML files and represented by `KitDefinition` + `KitManager`.
- Item loading already supports:
  - Serialized `ItemStack` entries.
  - Provider references (`provider` + `id` + `amount`) resolved by `CustomItemResolver`.
- GUI service currently focuses on player-facing browse/preview/claim and cancels top-inventory interaction.

## 3) Proposed UX

### 3.1 Entry points
Add new admin subcommands:
- `/ezkits edit <kit>`: Open editor for existing kit.
- `/ezkits create <kit>`: Create a draft kit and open editor.
- `/ezkits delete <kit>`: Delete kit with confirmation GUI.
- `/ezkits save <kit>` and `/ezkits discard <kit>` (optional command fallback to GUI buttons).

### 3.2 Editor GUI flow
1. **Kit List / Selector GUI**
   - Shows all kits with pagination and search/filter.
   - Left click = open editor.
   - Shift-left = duplicate.
   - Drop key / dedicated button = delete confirmation.
2. **Kit Metadata GUI**
   - Editable fields: display name, slot, permission, cooldown, one-time, preview-enabled, hidden, category, icon.
   - Use chat prompt or sign/anvil input for text/numeric fields.
3. **Kit Items Drag-and-Drop GUI**
   - Top inventory = kit contents grid (editable).
   - Bottom inventory = admin inventory source.
   - Admin drags/drops into kit grid; remove by taking out or using clear button.
   - Support stack split/merge semantics naturally from Bukkit inventory actions.
4. **Custom Item Tagging GUI / Action**
   - For selected slot, admin can mark item as provider-backed reference:
     - Choose provider (`nexo`, `executableitems`).
     - Enter provider item id.
     - Keep optional fallback serialized `ItemStack` preview for display.
   - Visual marker in lore/NBT metadata while editing so admins know item source mode.
5. **Review & Save GUI**
   - Diff summary against persisted kit.
   - Validate + save + reload only changed kit.

## 4) Data Model Changes

### 4.1 Introduce editor DTOs
Add mutable editor models separate from immutable runtime `KitDefinition`:
- `EditableKitDraft`
- `EditableKitItem`
  - `mode`: `SERIALIZED` | `CUSTOM_REF`
  - `itemStack` (for preview/edit representation)
  - `provider`, `providerId`, `amount` (when `CUSTOM_REF`)

Reason: runtime claim model stays simple while editor maintains richer state and unsaved edits.

### 4.2 Optional runtime enhancement
To avoid lossy round-trip for custom references, extend runtime definition with source-aware item entries:
- `KitItemEntry` sealed style object (`SerializedItemEntry`, `ProviderItemEntry`).
- Keep `safeItems()` compatibility by exposing resolved stacks for existing claim paths.

## 5) Persistence Strategy

### 5.1 Kit file writer
Add `KitFileWriter` to persist a single kit safely:
- Preserve canonical ordering of keys.
- Write temp file + atomic replace.
- Backup previous file (`.bak`) on successful write.

### 5.2 YAML format for items
Continue supporting existing schema and persist in explicit form:
- Serialized item:
  - full Bukkit `ItemStack` serialization block.
- Custom ref item:
  - `provider`, `id`, `amount`
  - optional `display` section for editor preview metadata (non-authoritative).

### 5.3 Migration
- Load old files unchanged.
- Save in normalized format after first edit.
- If unknown fields are present, keep them when possible or document unsupported fields in logs.

## 6) Inventory Event Handling Design

### 6.1 New menu types
Extend menu routing with admin types:
- `ADMIN_KIT_LIST`
- `ADMIN_KIT_EDIT_META`
- `ADMIN_KIT_EDIT_ITEMS`
- `ADMIN_KIT_EDIT_CUSTOM_ITEM`
- `ADMIN_KIT_CONFIRM_SAVE`
- `ADMIN_KIT_CONFIRM_DELETE`

### 6.2 Session management
Add `AdminEditorSessionService` keyed by admin UUID:
- active kit id/draft
- dirty flag
- last interaction timestamp
- pending text-input field

Session rules:
- auto-expire on quit or timeout.
- prevent concurrent edit collisions (simple lock per kit id).

### 6.3 Click/drag semantics
- For editor item grid, allow only specific actions and cancel dangerous actions (double-click collect, hotbar swap where needed).
- Implement `InventoryDragEvent` support (not only click events).
- Distinguish top vs bottom inventory; top is editable region.

## 7) Custom Item Support Plan

### 7.1 Input options
Provide two ways to create custom entries:
1. **Manual reference mode**
   - Select slot → set provider/id via prompt.
2. **Auto-detect mode (best effort)**
   - If held item contains known provider signatures/PDC tags, prefill provider + id.

### 7.2 Validation
- On save, resolve every custom ref through `CustomItemResolver`.
- If unresolved:
  - Block save by default with actionable message.
  - Optional config toggle to allow unresolved refs in offline-provider scenarios.

### 7.3 Claim-time behavior
- Keep existing resolver path.
- Improve warning messages with kit id + slot index + provider/id.

## 8) Permissions and Security
- New permissions:
  - `ezkits.admin.edit`
  - `ezkits.admin.create`
  - `ezkits.admin.delete`
  - `ezkits.admin.save`
- Restrict editor access to players only.
- Enforce per-action permission checks in GUI handlers (not just command layer).

## 9) Config + Messages
- Add configurable editor GUI layouts in `gui.yml` under `admin-editor.*`.
- Add text keys in `messages.yml` for:
  - session started/expired
  - invalid field value
  - save success/failure
  - conflict/lock notices
  - unresolved custom item errors

## 10) Development Phases

### Phase 1: Foundation
- Add command surface + permissions + message keys.
- Add editor session service and basic kit select GUI.

### Phase 2: Item Editor MVP
- Implement drag-and-drop kit items GUI for serialized item storage only.
- Save/discard workflow and YAML writer with backups.

### Phase 3: Metadata Editing
- Add metadata editor and prompts for text/number fields.
- Add validation and review screen.

### Phase 4: Custom Item References
- Add provider/id editor flow per slot.
- Save/load custom refs and validate on save.
- Optional auto-detect support.

### Phase 5: Hardening
- Edit lock conflicts, autosave warning, timeout behavior.
- Extensive event edge-case handling.
- Regression testing and docs/examples.

## 11) Testing Strategy

### Unit tests
- Kit YAML read/write round-trip (serialized + provider refs).
- Validation for metadata constraints and slot bounds.
- Session lifecycle and lock behavior.

### Integration tests (MockBukkit where possible)
- GUI click/drag behavior.
- Save and reload updated kit definitions.
- Custom provider ref validation pathways.

### Manual QA checklist
- Create kit from scratch in-game.
- Edit existing kit without data loss.
- Add/remove/reorder items by drag-and-drop.
- Mark item as custom provider ref and verify claim result.
- Permission denial paths and conflict handling.

## 12) Risks and Mitigations
- **Bukkit inventory edge cases:** Handle click + drag + hotbar events explicitly; add guard rails.
- **Data loss risk during save:** Use temp files + backups + post-write reload verification.
- **Provider API variability:** Keep reflection-based resolver and add provider-specific diagnostics.
- **Concurrent edits:** Lock kits and show active editor identity in error message.

## 13) Definition of Done
- Admin can create/edit/delete kits fully in-game.
- Drag-and-drop editing works for vanilla and custom provider-backed items.
- Saved kits persist correctly and reload without restart.
- No regressions in player claim/preview flow.
- Documentation updated with command usage and editor walkthrough.
