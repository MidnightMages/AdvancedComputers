local kernel = require("kernel")
local fs = require("filesystem")

kernel:debug("starting")
local proc = kernel:getCurrentProcess()
local args = proc.args
local dir = proc.cwd
if string.startsWith(args,"/") then
    dir = args
else
    dir = dir .. args
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
