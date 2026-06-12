# WorldTools (Updated Fork)

This project is a fork of the original [WorldTools](https://github.com/Avanatiker/WorldTools), updated to support newer Minecraft versions(1.21.5+).

For features and usage instructions, please refer to the original project.

## Bug Fixes

### "Overworld settings missing" when loading worlds from servers with custom dimensions

Servers that only expose custom dimensions (e.g. `play.hollowcube.net`) don't have a
`minecraft:overworld` dimension. When such a world was downloaded, the generated
`level.dat` lacked the `minecraft:overworld` entry that Minecraft's `WorldDimensions`
codec requires, so loading the world in singleplayer crashed with
`IllegalStateException: Overworld settings missing` ("Failed to load level data or
datapacks, can't proceed with server load").

Fixed by relabeling the first captured dimension as `minecraft:overworld` when no real
overworld is present, so the world loads correctly.
