# Move In Move Out - App Plan

## App Purpose
Move In Move Out helps renters and landlords document property condition during move-in and move-out, preserve timeline evidence, and generate fair deposit discussion reports.

## Screen Map
- `HomeActivity`
  - `MoveInWizardActivity` -> `CaptureActivity`
  - `MoveOutWizardActivity` -> `CaptureActivity`
  - `ReportActivity`
  - `TimelineActivity`
  - `RespectFilterActivity`
  - `LegalTranslatorActivity`
  - `SettingsActivity`

## Data Model Placeholders
- `Event`
  - id, type, timestamp, actor, notes
- `Property`
  - id, address, leaseStart, leaseEnd
- `Room`
  - id, propertyId, name, floor, notes
- `MediaItem`
  - id, roomId, eventId, uri, type, capturedAt, hash
- `TimelineEntry`
  - id, eventId, title, details, createdAt
- `Report`
  - id, propertyId, generatedAt, summary, lineItems, exportUri

## Milestones
1. Capture
   - Implement camera/photo/video capture with metadata timestamps.
2. Storage
   - Persist properties, rooms, events, and media locally (Room + file storage).
3. Comparisons
   - Add move-in vs move-out media pairing and condition deltas.
4. PDF Report
   - Generate exportable deposit fairness report with evidence references.
