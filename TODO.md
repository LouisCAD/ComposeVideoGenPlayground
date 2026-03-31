# TODO

## Priority (ordered)

- [x] Try transparent windows
- [x] Define the generation job model: `MediaGenJob`
- [x] Create `MediaGenJobList`
- [x] Create `addComposableToRecord`
- [x] Add UI to view ongoing MediaGenJob.
- [x] Adapt the existing video gen code (currently hardcoded for the counter overlay)
  - [x] Make it call `addComposableToRecord`
  - [x] Rename it to something okay
  - [x] Make it support any composable
- [x] **Create the sport-style score overlay**
- [ ] App-data
  - [ ] Create `appDataDir` extension
  - [ ] Save last video gen params?
  - [ ] Convert AndroidX AtomicFile to Kotlin/JVM or Okio Filesystem?
- [ ] Minimal Main UI (Composable picker)
  - [ ] Design it
  - [ ] Create the UI model(s)
  - [ ] Code a minimal version (should it have search)
- [ ] Create an ETA estimator function
  - It should take a flow + completion metadata
  - Create a state based adapter
- [ ] Abstract away resolution input
  - [ ] Introduce presets, sorted by orientation (maybe have 2160p, 1080p, etc, with portrait/landscape segmented button?)
- [ ] Create API for persistable WYSIWYG parametric composables.

## Other stuff

- [ ] Introduce live preview for overlays
