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

Fixed by sourcing the `level.dat` dimension registry from the dimensions actually
captured (the player's current dimension plus every dimension chunks were saved for)
instead of only `mc.connection.levels()` — which is empty on such servers — and adding
a synthetic empty `minecraft:overworld` when the server has none, so the captured custom
dimensions still load and the player spawns in their saved dimension.

### Downloaded world unusable in other game modes (can't move in creative)

Servers can hand out locked player state (e.g. `flySpeed` 0, `mayBuild` false, a
spectator game type) that was saved verbatim into the world, making it unusable in other
game modes — flying in creative is impossible with `flySpeed` 0.

Added a **World → Player Behavior → Modify Player Behavior** option (off by default) that,
when enabled, strips the server-imposed player abilities and game type when saving, so the
world's default game type and the vanilla per-game-mode ability defaults apply on load.
