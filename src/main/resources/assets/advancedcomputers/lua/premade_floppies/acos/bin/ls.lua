local kernel = require("kernel")
local fs = require("filesystem")

kernel:debug("starting")

local rawArgs = {...}
local argPath = #rawArgs > 0 and tostring(rawArgs[1]) or ""

---@type Process
local proc = kernel:getCurrentProcess()
local dir = proc.currentWorkingDirectory
if string.startsWith(argPath,"/") then
    dir = argPath
else
    dir = dir .. argPath
end

dir = kernel:normalizePath(dir .. "/")

if not fs:directoryExists(dir) then
    print("Directory '"..tostring(dir).."' does not exist")
    return 1
end
print("viewing '"..tostring(dir).."'")
if dir ~= "/" then
    print("./")
    print("../")
else
    print("/")
end
for _,f in ipairs(fs:list(dir)) do
    print(f)
end

return 0
