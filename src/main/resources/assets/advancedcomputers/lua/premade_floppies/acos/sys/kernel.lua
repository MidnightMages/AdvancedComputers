local computer = components:getFirst("computer")

---@type ProcessWithPrivateFields | nil
local currentlyRunningProcess = nil -- nil = kernelContext
local registeredPermissions = {}
function kutils.registerPermission(name)
    registeredPermissions[name] = true
end

function kutils.assertPermission(name)
    assert(registeredPermissions[name] ~= nil, "permission " .. tostring(name) .. " has not been registered!")
    return (currentlyRunningProcess == nil) --or (currentlyRunningProcess.euid == 0)
end

function kutils.getCurrentProcess()
    return currentlyRunningProcess
end

function kutils.resetToParent(screenIdx, screen, p, gpu)
    gpu = gpu or components:getFirst("gpu")
    if p == nil then
        gpu:assignBuffer(_ENV.uefiTextBuffer, screen)
        return
    end
    local activeBuf = kutils.unwrapObject(p).activeTextBuffers[screenIdx]
    if activeBuf ~= nil then
        gpu:assignBuffer(activeBuf, screen)
    else
        kutils.resetToParent(screenIdx, screen, p.parentProcess)
    end
end

local syscalls = {}

local function registerDriver(path)
    local newSyscalls = dofile(path)
    assert(type(newSyscalls) == "table", "driver did not return a syscall table")
    for key, value in pairs(newSyscalls) do
        assert(syscalls[key] == nil, "syscall " .. tostring(key) .. " is already registered!")
        syscalls[key] = value
    end
end

--[[
This is the base execution loop for the scheduler
]]


---@type ProcessWithPrivateFields[]
local runningProcesses = {}
_ENV.scheduler = {
    ---@type table<string, table<ProcessWithPrivateFields, function[]>>
    registeredEventCallbacksByTypeAndProcess = {}
}

function scheduler:enqueue(proc)
    assert(proc ~= nil, "proc was nil")
    table.insert(runningProcesses, proc)
end

function scheduler:block(blocked, blocking)

end

function scheduler:registerEventCallback(eventName, callbackFunc)
    if scheduler.registeredEventCallbacksByTypeAndProcess[eventName] == nil then
        scheduler.registeredEventCallbacksByTypeAndProcess[eventName] = {}
    end
    assert(currentlyRunningProcess ~= nil, "No process is running currently!")
    if scheduler.registeredEventCallbacksByTypeAndProcess[eventName][currentlyRunningProcess] == nil then
        scheduler.registeredEventCallbacksByTypeAndProcess[eventName][currentlyRunningProcess] = {}
    end
    table.insert(scheduler.registeredEventCallbacksByTypeAndProcess[eventName][currentlyRunningProcess], callbackFunc)
end

---@param process ProcessWithPrivateFields
---@param func function
---@param ... any
function scheduler:spawnNewThreadInProcess(process, func, ...) -- ... = thread start args
    process:createThread(func, table.pack(...))
end

registerDriver("/sys/drivers/process.lua")

function panic(msg)
    -- TODO
end

function doSyscall()
    -- TODO
end

local origCo = coroutine
_ENV["coroutine"] = {}

---@param f fun(...):...
---@return thread
---@diagnostic disable-next-line: duplicate-set-field
function coroutine.create(f)
    return origCo.create(function(...)
        return xpcall(f, debug.traceback)
    end)
end

---@param co thread
---@return boolean success
---@return ...
---@diagnostic disable-next-line: duplicate-set-field
function coroutine.resume(co, ...)
    return select(2, origCo.resume(co, ...)) -- first is always true as the co cannot fail
end

---@diagnostic disable-next-line: duplicate-set-field
function coroutine.yield(...)
    return origCo.yield(true, ...)
end

local sleep = _ENV.sleep


print("new kernel running!!!!!!")
local lastCnt = -1
local function runTasks()
    while true do
        -- process events
        while true do
            local machineEvent = { computer:getMachineEvent() }
            if #machineEvent == 0 then break end -- no event available

            if machineEvent[1] == "shutdown" then
                return
            end

            for _, process in ipairs(runningProcesses) do -- walk through all registered handlers and spawn new threads
                for i = 1, 2 do
                    local handlers = (scheduler.registeredEventCallbacksByTypeAndProcess[i == 1 and "*" or machineEvent[1]] or {})
                        [process] or {}
                    for j = 1, #handlers do
                        --print("created event thread :)", table.unpack(machineEvent))
                        scheduler:spawnNewThreadInProcess(process, handlers[j], table.unpack(machineEvent))
                    end
                end
            end
            -- resume all eventhandlers
        end

        if lastCnt ~= #runningProcesses then
            --print("proc count: ", lastCnt, "-->", #runningProcesses)
            lastCnt = #runningProcesses
        end
        local diedProcessIds = {}

        local seenRootProc = false
        for i = 1, #runningProcesses do
            local processToRun = runningProcesses[i]
            if processToRun.id == 0 then -- found the root process
                seenRootProc = true
            end
            local function markCurrentProcessForErrorKilling()
                processToRun.endedSuccessfully = false
                table.insert(diedProcessIds, i)
            end
            currentlyRunningProcess = processToRun
            local unblockedThreads = processToRun.unblockedThreads
            for j = 1, #unblockedThreads do
                local currThreadToRun = unblockedThreads[j]
                if (currThreadToRun.pausedUntil or -1) < computer:getEpoch() then
                    --print("resuming with args: ", currThreadToRun.coroutine_resumptionArgs)
                    local result = table.pack(coroutine.resume(currThreadToRun.coroutine,
                        table.unpack(currThreadToRun.coroutine_resumptionArgs or {})))
                    if type(result[3]) == "string" and result[3] ~= "sleep" then
                        --print("PACKED: ", table.unpack(result))
                    end
                    -- handle syscalls / result
                    if not result[1] then -- if error
                        -- TODO kill process
                        print("we need to kill a process (proc errored) :(\nInitial error: " ..
                            tostring(result[2]) .. ":" .. tostring(result[3]))
                        markCurrentProcessForErrorKilling(); break
                    else -- success
                        if origCo.status(currThreadToRun.coroutine) == "dead" then
                            --print("a thread of '"..tostring(processToRun.description).."' has ended. removing.")
                            unblockedThreads[j] = nil
                        else
                            local action = result[2]
                            if action == "syscall" then
                                local syscallName = result[3]
                                --print("syscall name", syscallName)
                                if syscallName == "sleep" then
                                    local sleepDuration = result[4]
                                    assert(type(sleepDuration) == "number")
                                    currThreadToRun.pausedUntil = computer:getEpoch() + tonumber(sleepDuration)
                                else
                                    local syscallFunc = syscalls[syscallName]
                                    if syscallFunc then
                                        currThreadToRun.coroutine_resumptionArgs = table.pack(syscallFunc(table.unpack(
                                            result, 4)))
                                    else
                                        print("we need to kill a process (bad syscall name) " ..
                                            tostring(syscallName) .. " :(")
                                        markCurrentProcessForErrorKilling(); break
                                    end
                                end
                            else
                                print("we need to kill a process (bad action) " .. tostring(action) .. " :(")
                                markCurrentProcessForErrorKilling(); break
                            end
                        end
                    end
                end
            end

            local pre = #unblockedThreads
            for j = #unblockedThreads, 1, -1 do
                if unblockedThreads[j] == nil then
                    table.remove(unblockedThreads, j)
                end
            end
            if #unblockedThreads ~= pre then
                --print("unblocked thread cnt", pre, "-->", #unblockedThreads)
            end
            if #unblockedThreads == 0 then -- no more threads --> process is dead
                table.insert(diedProcessIds, i)
            end
            currentlyRunningProcess = nil
            sleep(0.05)
        end

        for j = #diedProcessIds, 1, -1 do
            local diedPid_aka_i = diedProcessIds[j]
            local procObj = runningProcesses[diedPid_aka_i]
            procObj.endedSuccessfully = procObj.endedSuccessfully ~=
                false -- we have set this to false already if the process errored. So if it was not set, all was well
            procObj.state = PROCESS_RUNSTATE.dead
            -- reset used screens
            local k = 1
            for type, component in components:list() do
                if type == "screen" then
                    if procObj.activeTextBuffers[k] ~= nil then
                        kutils.resetToParent(k, component, procObj.parentProcess, gpu)
                        procObj.activeTextBuffers[k] = nil
                    end
                    procObj[k] = nil
                    k = k + 1
                end
            end
            for _, buffer in ipairs(procObj.textBuffers) do
                if buffer.isAlive then
                    buffer:free()
                end
            end
            --print("marked process '"..tostring(procObj.description).."' as dead")
            table.remove(runningProcesses, diedPid_aka_i)
        end

        if not seenRootProc then -- kill the scheduler if the root process dies
            break
        end
    end
end

---@type FullProcessStartInfo
local initProcessStartInfo = {
    currentWorkingDirectory = "/bin/",
    mainFunc = assert(loadfile("/bin/sh.lua")),
    args = {},
    description = "init shell"
}

local function invokeSyscall(syscallName, ...)
    coroutine.yield("syscall", syscallName, ...)
end

---@param duration number Duration in seconds
_G["sleep"] = function(duration)
    invokeSyscall("sleep", duration)
end

local kernelCoroutine = origCo.running()
syscalls["sleep"] = function()
end

--[[
local function readonlyView(x)
    return setmetatable({}, {
        __index = function(_, k)
            local rv = rawget(x, k)
            print("access: ", k, "-->", rv)
            if type(rv) == "table" then return readonlyView(rv) end
            return rv
        end,
        __metatable = false
    })
end]]

syscalls["getCurrentProcess"] = function()
    assert(currentlyRunningProcess)
    return (currentlyRunningProcess)
end

syscalls["registerProcessEventCallback"] = function(eventName, callback)
    scheduler:registerEventCallback(eventName, callback)
end

function syscalls.allocTextBuffer(proc, width, height)
    width = width or 110  -- default width
    height = height or 44 -- default height
    local gpu = components:getFirst("gpu")
    local ok, buffer = pcall(function() return gpu:newBuffer(width, height) end)
    if ok then
        local procWithPrivate = kutils.unwrapObject(proc)
        procWithPrivate.textBuffers[buffer] = true
        return buffer
    else
        return nil, buffer -- return nil and the error message
    end
end

function syscalls.showTextBuffer(proc, textBuffer, ...)
    local procWithPrivate = kutils.unwrapObject(proc)
    if textBuffer ~= nil and not procWithPrivate.textBuffers[textBuffer] then
        error("this process does not own the given text buffer")
    end
    local gpu = components:getFirst("gpu")
    if select("#", ...) then
        -- all screens are affected
        if textBuffer == nil then
            -- reset all screens to parent
            local i = 1
            for type, component in components:list() do
                if type == "screen" then
                    kutils.resetToParent(i, component, procWithPrivate.parentProcess, gpu)
                    procWithPrivate.activeTextBuffers[i] = nil
                    i = i + 1
                end
            end
        else
            -- set all the screens
            local i = 1
            for type, component in components:list() do
                if type == "screen" then
                    gpu:assignBuffer(textBuffer, component)
                    procWithPrivate.activeTextBuffers[i] = textBuffer
                    i = i + 1
                end
            end
        end
    else
        -- only some screens are affected
        local restructured = {}
        for _, target in ipairs(table.pack(...)) do
            restructured[target] = true
        end
        if textBuffer == nil then
            -- reset only some screens to parent
            local i = 1
            for type, component in components:list() do
                if type == "screen" then
                    if restructured[i] then
                        kutils.resetToParent(i, component, procWithPrivate.parentProcess, gpu)
                        procWithPrivate.activeTextBuffers[i] = nil
                    end
                    i = i + 1
                end
            end
        else
            -- set only some screens
            local i = 1
            for type, component in components:list() do
                if type == "screen" then
                    if restructured[i] then
                        gpu:assignBuffer(textBuffer, component)
                        procWithPrivate.activeTextBuffers[i] = textBuffer
                    end
                    i = i + 1
                end
            end
        end
    end
end

syscalls.spawnProcess(initProcessStartInfo)


runTasks()
print("shutting down ...")
