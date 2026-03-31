local kernel = require("kernel")
local fs = require("filesystem")

kernel:debug("starting")
local proc = kernel:getCurrentProcess()
local args = proc.args


if #args == 0 then
    print("Given filepath is empty. Usage: pasteToFile.lua /some/path/to/write/to")
    return 1
end

local cwd = proc.cwd
local filePath
if string.startsWith(args,"/") then
    filePath = args
else
    filePath = cwd .. args
end

filePath = kernel:normalizePath(filePath)

if fs:fileExists(filePath) then
    print("File '"..tostring(filePath).."' already exists")
    return 1
end

print("Please paste the text that you want to write to the file (via middle mouse button). To abort, press ENTER.")

local function textPasted(str)
    fs:writeAllText(filePath, str)
end

local keepRunning = true
kernel:registerEventCallback("charTyped", function(...)
    if not keepRunning then end -- if already pasting something
    if (select(2,...)) == '\n' then keepRunning = false end
end)

kernel:registerEventCallback("textPasted", function(...)
    if not keepRunning then end -- if already aborted or pasted
    print("text received, writing to file...")
    keepRunning = false
    textPasted(select(2,...))
end)
while keepRunning do sleep(1) end

return 0
