# Compose Video Generation Playground

This project contains several experiments:

## compose-to-video

Record Compose Desktop composables as **transparent** videos.

Here's what the project does (in streaming):
- Run the animations (at max speed, i.e., faster than realtime)
- Take frames snapshots
- Encode them to transparent lossless WEBP images in parallel (as much as the CPU and storage can handle)
- Feeds them to ffmpeg to generate an Apple ProRes 4444 transparent video (once all WEBPs are encoded)

Currently, the only example is a counter, where you feed a list of timecodes (hh:mm:ss:frame_number) as a text file, and you get a nicely animated transparent video with the requested resolution and density.

### How to try it out:

1. Run the app with `./gradlew :compose-to-video:run`
2. Drag a timecode list file looking like this:
```
30 // 30th frame in the 1st second
0:45 // 45th frame in the 1st second
1:0 // BTW, comments are allowed
1:30
2:00 // Leading zeros too
4:00
4:30
5:00
```
3. Drag an empty directory for the output
4. Click the obvious button
5. Wait until the progress screen goes away to reshow the main screen content
6. Check the output video in the directory you chose.

## feather-borders

This experiment feathers the borders of images using Compose.
It means that the edges have a transparent gradient, to make the image
nice to integrate on top of a background of a different color, without sharp edges.
