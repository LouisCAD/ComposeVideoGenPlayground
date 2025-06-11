# Compose Video Generation Playground

This project contains several experiments:

## compose-to-video

Record Compose Desktop composables as videos.

It runs the animations,
takes frames snapshots,
encodes them to transparent lossless WEBP images in parallel,
and feeds them to ffmpeg to generate an Apple ProRes 4444 transparent video.

Currently, the only example is a counter, where you feed a list of timecodes (hh:mm:ss:frame_number) as a text file, and you get a nicely animated transparent video with the requested resolution and density.

## feather-borders

This experiment feathers the borders of images using Compose.
It means that the edges have a transparent gradient, to make the image
nice to integrate on top of a background of a different color, without sharp edges.
