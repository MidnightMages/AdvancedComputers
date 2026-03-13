# Advanced Computers
This is yet another computer mod for Minecraft, including a *custom built LUA runtime*, called [JLuaVm](https://github.com/MidnightMages/JLuaVM), and thus also the ability to *persist computer states* across chunk unloading and even server shutdowns, meaning they wont be forcefully rebooted in such events, similar to OpenComputers' 1.12 behaviour. 
This is of course inspired by OpenComputers 1.12 and the original ComputerCraft mod.

Feel free to join our Discord server for support or any kind of questions: https://discord.gg/YBvURmhN9f

## THIS IS AN ALPHA
At this point there is still some fundamental content missing (thus the alpha tag) and there might also be a number of bugs present (please open an issue ticket if you find one).

## Features
Currently adds programmable computers into Minecraft, along with screens, peripheral cables, real-world-internet-access, and more, all being interactable through Lua 5.4.
As far as resource usage goes, we are only setting up a lua execution environment per computer and **not** an entire virtual machine (that would then run linux) as other mods do. This means, our computers are much more lightweight in terms of ram and cpu usage.

Planned stuff (for deeming the mod fit for Beta):
- Finish adding anything that is missing from the Lua standard library (most notably some edgecase features in pattern matching, i.e string.gsub and related functions, though they mostly work)
- Sending network packets between ingame computers (with some intelligent packet handling to avoid having to write a custom, ingame IP protocol) 
- Making a wiki that lists & describes all the api functions
- Interaction with more minecraft blocks
- Make disk space limited and add a soft computer ram limit
- Adding ingame networking (computer to computer)
- Adding more fun stuff like servers, etc.

## Targeted minecraft versions
Currently this targets Minecraft 1.20.1 (forge), but we do plan to extend that, once the mod is reaches the Release state (i.e. containing very few bugs and being mostly content complete).

## License & Attribution
Feel free to include this mod in any modpacks. Though we do ask you not to publicly re-upload *individual* mod builds, reuploading them in the form of a modpack, no matter how small (as long as it contains some other mod or forge/neoforge), is fine. It would be nice if you could send us a message in Discord if you did use this mod as part of a modpack, as we are curious :D.

As specified in the LICENSE file, the mod's is published under the MIT license. An exception to this are the fonts, which are located in /assetSources/font, for those, the associated license in that folder applies.


## Contributing
Bugfixes are always welcome in the form of Pull-Requests (For the mod and JLuaVm). For adding new features in the form of a PR, please get in touch with us on Discord (see link above) first to avoid doing duplicate work, etc (especially before the mod is content complete).

## Lua 5.4 details
This mod internally relies on another project of ours, which we solely started because of the lack of java-based Lua runtimes that also support state serialization.
The runtime is called JLuaVm, written entirely in java, from scratch, and supports all of Lua 5.4 except for:
- Weak tables
- Most of the debug library
- Anything that would interact with the host system (as it is meant to be sandboxed). Functionality like writing to disk is implemented by this mod itself, for example.
- Type extension functions, essentially allowing for the same functionality as using __index of debug.setmetatable() for types like string, boolean, etc., but on a per-_ENV basis rather than globally.
