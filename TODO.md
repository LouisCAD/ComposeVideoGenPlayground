# TODO

## Priority (ordered)

- [x] Try transparent windows
- [x] Define the generation job model: `MediaGenJob`
- [x] Create `MediaGenJobList`
- [x] Create `addComposableToRecord`
- [ ] Add UI (& previews) to view ongoing MediaGenJob.
- [ ] Adapt the existing video gen code (currently hardcoded for the counter overlay)
  - [ ] Make it call `addComposableToRecord`
  - [ ] Rename it to something like "timecode list based video gen"?
  - [ ] Make it support any composable
- [ ] Minimal Main UI
  - [ ] Design it
  - [ ] Create the UI model(s)
  - [ ] Code a minimal version (should it have search)
- [ ] Create an ETA estimator function
  - It should take a flow + completion metadata
  - Create a state based adapter
- [ ] Abstract away resolution input
  - [ ] Introduce presets, sorted by orientation (maybe have 2160p, 1080p, etc, with portrait/landscape segmented button?)
- [ ] Create the sport score overlay

## Other stuff

- [ ] Introduce live preview for overlays
