local kernel = require("kernel")
local fs = require("filesystem")

kernel:debug("starting")
local proc = kernel:getCurrentProcess()
local args = proc.args
local cwd = proc.cwd
local filePath
if string.startsWith(args,"/") then
    filePath = args
else
    filePath = cwd .. args
end

filePath = kernel:normalizePath(filePath)
if filePath:endsWith("/") then
    if not fs:directoryExists(filePath) then
        print("Directory '"..tostring(filePath).."' does not exist, thus cannot remove it.")
        return 1
    end
    fs:delete(filePath)
else
    if not fs:fileExists(filePath) then
        print("File '"..tostring(filePath).."' does not exist, thus cannot remove it.")
        return 1
    end
    fs:delete(filePath)
end



return 0
