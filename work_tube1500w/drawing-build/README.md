# Rebuilding TUBE1500W.g

`TUBE1500W.g` (main synoptic, layout of 2026-11-23, H2O line of 2026-11-27) is generated from the previous
drawing (commit 491178f2) by `NewMain.java`, using the GLG API in `lib/GlgCE.jar`.
Needs an X display. Run from `work_tube1500w` with Java 8:

```
git show 491178f2:work_tube1500w/TUBE1500W.g > /tmp/orig.g
javac -cp "../lib/*" -d /tmp/build drawing-build/*.java
java -cp "/tmp/build:../lib/*" NewMain /tmp/orig.g TUBE1500W.g      # build
java -cp "/tmp/build:../lib/*" SnapM TUBE1500W.g /tmp/synoptic.png   # picture of the result
```

If a program does not return after printing `saved=true`, stop it (or run it
under `timeout 60`): the drawing is already written.

`Check.java` verifies that every name the panel drives resolves in the drawing:
it takes the drawing and a text file with one key of `TUBE1500WSTATUS`
(`ChannelList.java`) per line.

Coordinates in `NewMain.java` are in the 1600 x 700 space of the drawing.
`X1` is the V21-P22 line, `X2` the V1-PS line.
