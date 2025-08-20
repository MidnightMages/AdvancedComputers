---
name: Bug report
about: Report a bug / crash
title: ''
labels: bug
assignees: ''

---

<!--
IMPORTANT:
ONLY USE THIS TEMPLATE FOR ACTUAL (or potential) BUGS. If this is a question, open an issue without using an issue-template.
PLEASE FOLLOW THIS TEMPLATE AS CLOSELY AS POSSIBLE. Lines starting with # should be kept in place, as well as the 'Environment' section structure as a whole, feel free to modify the rest as you see fit.
Not following the template (without a very good reason) or misusing it, may result in your ticket being closed.
-->

# Description of the bug
## Current behaviour
<!-- replace the following lines with your own -->
**Describe your issue as clearly and in as much detail as possible.**\
The goal here is that we do not need to ask you for any extra info, which makes our life easier and speeds things up, meaning it gets fixed quicker.

For example, if textures of a certain block are broken, do **not** simply write `textures of block A are broken` or `textures are broken`

Instead add enough info that the issue is so obvious that it is absolutely impossible to misunderstand you. A much better way to describe it would be:
`The texture of the left face of block A is wrongly rotated by 90 degrees` or `The top side of block A contains a misplaced pixel as you can see in the following screenshot <screenshot here>.` - though in the latter case please clearly mark which pixel you are referring to, by editing the screenshot using paint or whatever.

Similarly, don't simply say `my game crashed`, instead explain
- what you were doing right when it crashed (as that may have caused it or played a role indirectly), 
- how it crashed: did it freeze for a while, did the game just close immediately, were there any suspicious glitches right before, etc.

Thirdly, and probably **most importantly** if your description ends up being `X does not work`, it is a _very_ bad description, as we will then have to inquire about *why* it does not work and what the actual behaviour is instead.
<!----------------------------------------------->

## Expected behaviour
<!-- replace the following lines with your own -->
Often it might not be clear what behaviour you did expect/wanted to achieve, e.g. if you state, that a certain LUA function wrongly returns a string in a certain case, it would be very useful if you could state what you did expect it to return.
<!----------------------------------------------->

# Reproduction steps
<!-- replace the following lines with your own -->
This is often the most important section. If you can clearly reproduce a complex issue <ins>and describe it to us in a way so that we can also do so</ins>, it can _really_ speed up fixing, potentially saving us tens of hours of testing.

Ideally you would start from a clean state, which menas not using an existing world and restarting the client and server before testing, to rule out state carrying over. 

It _is_ however understandable if you do not want to go through that process. In most cases it should be sufficient to just make a new world and test there.
Please do not use an existing world as we do not have access to that world and also do not want to install it, along with potentially 200 other mods. 

Reproduction steps should look similar to this, and as before, make it as obvious as possible to us:
1. Create a new world
0. Place down a computer of type X and place down a screen on top
0. Start the computer by opening up the UI and clicking the 'start' button.
0. Observe X (e.g. the game crashing or behaviour Y appearing, etc. But again, please be specific. **This observation-part is important**)

If, for example, the issue is isolated to a lua function not working properly, across different computer setups, you can simply give us a lua snippet to run that causes the wrong behaviour.

In those cases it might look something like this:
1. Install operating system X on computer of type Y
2. Type `edit TestFile.lua` and hit Enter
3. Paste in the code linked below
4. Type `TestFile` and hit Enter
5. Observe that the in-game computer crashes, when the snippet instead should have executed without error


Code to be put into TestFile.lua:
```lua
if true then local a1 end
local f
f = function() return f end
```

For such lua related issues, please try to provide an example that is as simple and small as possible. Often an issue in a 1000 line codebase can be reproduced by just a few lines instead (as was the case in the above snippet).

<!----------------------------------------------->

# Environment
<!-- replace the INFO of the following lines with your own, keep the overall structure -->
- Minecraft version: 1.20.1 <!-- this is the only supported version currently-->
- Advanced Computers mod version: 1.2.3.4 <!-- This version is mentioned in the logfiles and is also included in the mod's .jar name -->
- Are other mods installed, besides Advanced computers and **its own** dependencies (e.g. forge/neoforge)? YES/NO
- Were you playing in a multiplayer game while the issue was happening? Singleplayer/OpenToLan/DedicatedServer
<!-------------------------------------------------------------->


# Log files & console output
<!-- replace the following lines with your own -->
If you see anything suspicious in the client or server console, please include it here (if it exceeds more than a few lines, please upload it as a text file instead).

Please also always include the client **and** server logfiles for when the issue occurred. Reason for this is that in-game computer issues may produce errors that we can see in those log files, and generally Minecraft spits out errors when it is unhappy about something, which helps a lot in figuring out issues. 

An exception for when we would not need log is if we can indeed reliably reproduce your issue in a **singleplayer** environment, so **if you are confident**, feel free to skip this step. Therefore, if you have not tried to / were not able to reproduce it in singleplayer, please do include the log files as stated previously.

If you do decide to upload log files, again, please upload them as files. Do not paste hundreds of lines of text here.
<!----------------------------------------------->

# Extra info <!-- feel free to remove this section if unneeded -->

This section is for anything that does not fit into the above sections and for some extra notes.

Feel free to include screenshots/videos whereever to illustrate your points.

Make sure you have removed any unneeded template code<!-- and thanks for reading all of this -->.
