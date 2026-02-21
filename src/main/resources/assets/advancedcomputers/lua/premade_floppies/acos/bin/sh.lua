local kernel = require("kernel")
local fs = require("filesystem")
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
    if statement == "exit" then return true end

    local splitted = string.split(statement, " ")
    local executablePath = splitted[1]
    local argString = table.concat(splitted, " ", 2)

    if executablePath == "cd" then
        local firstChar = argString:sub(1,1)
        local path = argString
        if firstChar ~= "~" and firstChar ~= "/" then -- if relative
            path = kernel:getCurrentWorkingDirectory()..path
        end
        kernel:setCurrentWorkingDirectory(path)
    else
        local dstPath = "/bin/"..executablePath..".lua"
        if fs:fileExists(dstPath) then           
            local proc = kernel:startProcessFromPath(dstPath, argString)
            local res = kernel:waitForProcessExit(proc)
            kernel:debug("result:", res[1] == true and "success" or "error", select(2, table.unpack(res))) 
        else
            print("ERROR: file '"..tostring(dstPath).."' does not exist")
        end
    end
end

if argString ~= "" then
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
local function keyTyped(key) -- return whether to exit
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
kernel:registerEventCallback("keyTyped", function(...)
    if keyTyped(select(2,...)) then keepRunning = false end
end)
while keepRunning do
    sleep(5);
end