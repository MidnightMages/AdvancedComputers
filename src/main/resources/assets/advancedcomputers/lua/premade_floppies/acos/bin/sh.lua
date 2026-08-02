local kernel = require("kernel")
local fs = require("filesystem")
    print("a")
local argString = kernel:getCurrentProcess().args
local function printPrefix()
    local path = kernel:getCurrentWorkingDirectory()
    assert(#path>0, "cwd was empty?")
    if path:sub(1,1) ~= "/" then
        path = "/"..path
    end
    printInline("root@hostname:"..tostring(path).."# ")
end

kernel:debug("Shell with PID", kernel:getCurrentProcess().pid, "was started")

local function executeStatement(statement)
    print("Shell is executing statement", statement)
    if statement == "exit" then return true end
    local splitted = string.split(statement, " ")
    local executablePath = splitted[1]
    local argString = #splitted > 0 and table.move(splitted, 2, #splitted, 1, {}) or {}

    if executablePath == "cd" then
        local path = table.concat(argString, " ")        
        if #path == 0 then
            print("ERROR: cd requires an argument being the path to cd to.")
            return
        end
        local firstChar = path:sub(1,1)
        if firstChar ~= "~" and firstChar ~= "/" then -- if relative
            path = kernel:getCurrentWorkingDirectory()..path
            print("concatted with cwd into ", path)
        end
        path = kernel:normalizePath(path)
        if not fs:directoryExists(path) then
            print("ERROR: cannot cd to directory "..tostring(path).. " as it does not exist.")
            return
        end
        kernel:setCurrentWorkingDirectory(path)
    else
        local dstPath = "/bin/"..executablePath..".lua"
        if fs:fileExists(dstPath) then           
            local proc = kernel:startProcessFromPath(dstPath, argString)
            local wasSuccess = kernel:waitForProcessExit(proc)

            kernel:debug("result:", wasSuccess and "success" or "error") 
        else
            print("ERROR: file '"..tostring(dstPath).."' does not exist")
        end
    end
end

if (argString or "") ~= "" then
    executeStatement(argString)
    return 0
end

local rcFile = "/root/.bongorc"
if fs:fileExists(rcFile) then
    local rcFileContents = fs:readAllText(rcFile)
    executeStatement(rcFileContents)
end

local captureInput = true
local stringBuffer = ""
local function charTyped(key) -- return whether to exit
    if not captureInput then return end
    if key == "\b" then
        if #stringBuffer > 0 then
            printInline(key)
            stringBuffer = stringBuffer:sub(1, #stringBuffer - 1)
        end
    else
        printInline(key)
    end

    if key == "\n" then
        local statement = stringBuffer
        stringBuffer = ""
        captureInput = false
        local res = executeStatement(statement)
        captureInput = true
        if res == true then return true end
        printPrefix()
    elseif key ~= "\b" then
        stringBuffer = stringBuffer .. key
    end
end

printInline("-----------\n")
printInline("Bongo Shell\n")

printPrefix()

local keepRunning = true
kernel:registerEventCallback("charTyped", function(...)
    if charTyped(select(2,...)) then keepRunning = false end
end)
while keepRunning do
    sleep(5);
end