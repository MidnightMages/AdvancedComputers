local kernel = {}

local computer = component:getFirst("computer")

---@class processStartData : table
---@field priority integer
---@field coroutines thread[]
---@field cwd string
---@field resumeAfter number
---@field args string

---@class process : processStartData
---@field pid integer
---@field handle processHandle

---@class processHandle
---@field state string
---@field result any

---@type process[]
local processes = {}

-- todo add event loop, and then keep resuming coroutines; replace the sleep function to be coroutine.yield; replace coroutine.create such that sleep works in child coroutines too

local shutdownRequested = false
local eventQueue = {}
--[[local function eventPump()
    local event, a1, a2 = computer.getMachineEvent()
    if event ~= nil then        
        if event == "shutdown" then

        elseif event == "keyTyped" then
            if keyTyped(a1) then break end
        end
    end
end]]

---@class eventHandler
---@field func function
---@field process process

--eventId : delegate[]
---@type table<string, eventHandler[]> -- eventName, data
local eventHandlers = {}

local time = 0
local function os_time() -- TODO replace with computer.time() once it is implemented
    return time
end

local enableDebugLogging = false
local function debugf(...)
    if enableDebugLogging then
        print(...)
    end
end
function kernel:debug(...) debugf(...) end

---@type process?
local currProcess = nil
function kernel:registerEventCallback(eventName, callback) -- TODO add unregister function
    local container = eventHandlers[eventName]
    if not container then
        container = {}
        eventHandlers[eventName] = container
    end
    assert(currProcess, "registerEvent curr proc is nil")
    table.insert(container, {func=callback, process = currProcess})
end

local function coroutineXPCreate(f)
    return coroutine.create(function()
        local res = table.pack(xpcall(f, debug.traceback))
        --print("call done")
        if not res[1] then
            print("erroring")
            error(res[2])
        end
        --print("all good")
        return table.unpack(select(1,res))
    end)
end

local kernelRootCoroutine = coroutine.running()
debugf("main was: ", coroutine.running())
local sleepRaw = sleep
local run_skipCurrentSleep = false
function kernel:run()
    --kernel:startProcess({priority=0, coroutine=coroutineXPCreate(eventPump), cwd="/"})
    
    local processIdleTimeLeft = 0
    while not shutdownRequested do

        local eventTriggered = false
        while true do -- process event queue always
            local nextEvent = table.pack(computer:getMachineEvent())
            if nextEvent[1] == nil then break end
            if nextEvent[1] == "shutdown" then
                shutdownRequested = true
            end
            local container = eventHandlers[nextEvent[1]]
            if container then
                for _, eh in ipairs(container) do
                    --currProcess = eh.process
                    for i = 1, #eh.process.coroutines do
                        assert(coroutine.status(eh.process.coroutines[i]) ~= "dead", "a coroutine was dead and was not cleaned up2")
                    end
                    --print("interrupt set up for proc id", eh.process.pid)
                    eventTriggered = true
                    table.insert(eh.process.coroutines, coroutineXPCreate(
                        function()
                            eh.func(table.unpack(nextEvent))
                        end))
                    for i = 1, #eh.process.coroutines do
                        assert(coroutine.status(eh.process.coroutines[i]) ~= "dead", "a dead coroutine was added")
                    end
                    --currProcess = nil
                end
            end
        end
        assert(currProcess == nil, "kernel currProc was not reset")
        if processIdleTimeLeft <= 0 or eventTriggered then
            local deadProcesses = {}
            local earliestResume = nil
            local processCopy = {}
            for idx, proc in ipairs(processes) do
                processCopy[idx] = proc
            end


            --print("proc queue", #processCopy)
            for procIdx, proc in pairs(processCopy) do
                --print("processing proc ", proc.pid)
                if (proc.resumeAfter < os_time()) or (#proc.coroutines > 1) then
                    local deadCoroutineIndices = {}
                    local deadCoroutineIndices_len = 0
                    local cosToResume = {}
                    for i = 2, #proc.coroutines do
                        table.insert(cosToResume, {i, proc.coroutines[i]})
                    end
                    table.insert(cosToResume, {1, proc.coroutines[1]})
                    currProcess = proc -- TODO lock this table or make a clone, so that it cannot be edited
                    for ci, kv in ipairs(cosToResume) do
                        local coToResume = kv[2]
                        local coToResumeIdx = kv[1]
                        local isInterrupt = ci < #cosToResume
                        assert(coToResume ~= nil, "coroutine to resume was nil "..tostring(#proc.coroutines)..","..tostring(proc.pid))
                        local resState = coroutine.status(coToResume)
                        if resState ~= "suspended" then
                            print("Coroutine is in abnormal state: ", resState, #proc.coroutines)
                        end

                        -- success and reults
                        local rv = table.pack(coroutine.resume(coToResume))
                        local cores = coroutine.status(coToResume)
                        --if isInterrupt and cores ~= "dead" then -- good for finding deadlocks
                            --print("pid 1 interrupt res", cores)
                        --end
                        if not rv[1] then
                            --print("[warn] co errored:", rv[2])							
                            error("[warn as error] co errored: \n"..tostring(rv[2]))
                        end

                        -- remove coroutine from processes if the coroutine has entered the dead state -> the process has exited
                        if coroutine.status(coToResume) == "dead" then
                            --for i = 1, #proc.coroutines, 1 do
                            --   print(i, coroutine.status(proc.coroutines[i]) )
                            --end
                            --print("removing id", coToResumeIdx)
                            deadCoroutineIndices[coToResumeIdx] = true
                            deadCoroutineIndices_len = deadCoroutineIndices_len + 1

                            --table.remove(proc.coroutines, resumptionIdx)
                            for i = 1, #proc.coroutines do                                
                                assert(coroutine.status(proc.coroutines[i]) ~= "dead" or deadCoroutineIndices[i], "a coroutine was dead and was not cleaned up")
                            end
                            local aliveCoroutineCnt = #proc.coroutines - deadCoroutineIndices_len
                            --print("counts",#proc.coroutines, deadCoroutineIndices_len)
                            if coToResumeIdx == 1 then
                                assert(aliveCoroutineCnt == 0, "cleaned up root co before interrupts")
                            end

                            --print("dead", resumptionIdx)
                            if aliveCoroutineCnt == 0 then -- if no more coroutines, then process has died
                                proc.handle.result = rv
                                proc.handle.state = "dead"
                                table.insert(deadProcesses, procIdx)
                                debugf("process with pid "..tostring(proc.pid).." and idx "..tostring(procIdx)..  " has exited")
                            end
                            -- remove the dead coroutine
                            --print("rem func", table.remove)
                            --print("deleted dead co")
                        end
                    end

                    for i = #proc.coroutines,1,-1 do
                        if deadCoroutineIndices[i] then
                            --print("deleting idx", i)
                            table.remove(proc.coroutines, i)
                        end
                    end

                    for i = 1, #proc.coroutines do                                
                        assert(coroutine.status(proc.coroutines[i]) ~= "dead" or deadCoroutineIndices[i], "a coroutine was dead and was not cleaned up3")
                    end

                    local resumeAt =  currProcess.resumeAfter
                    earliestResume = earliestResume and math.min(earliestResume, resumeAt) or resumeAt
                    currProcess = nil
                end
            end
            for i = 1, #deadProcesses do
                table.remove(processes, deadProcesses[i])
                earliestResume = nil -- if a process died, force instant resume as some might have become unblocked
            end

            for i = 1, #processes do
                assert(processes[i].handle.state ~= "dead", "found a dead process"..tostring(i))
            end

            processIdleTimeLeft = math.min(0.1, earliestResume and (earliestResume-os_time()) or 0) -- pause process queue execution at most for one second
            if run_skipCurrentSleep then processIdleTimeLeft = 0 end
        end
        
        local sleepAmount = 0.05
        sleepRaw(sleepAmount)
        time = time + sleepAmount
        processIdleTimeLeft = processIdleTimeLeft - sleepAmount
    end
end

local newCo = coroutine.running()
--print(newCo, kernelRootCoroutine, newCo == kernelRootCoroutine)
--print("sanity check", coroutine.running() == coroutine.running())

---@param duration number Duration in seconds
_G["sleep"] = function(duration)
    --print("curr sleep proc: ", tostring(currProcess))
    currProcess.resumeAfter = os_time() + math.max(0, duration)
    --print("yielding")
    assert(select(2,coroutine.running()) == false, "attempted to yield kernel coroutine")
    --print(coroutine.running())
    --print("yielding")
    coroutine.yield()
end

---@return process
function kernel:getCurrentProcess() return assert(currProcess) end

local nextPid = 1
---@param proc processStartData
function kernel:startProcess(proc)
    local pid = nextPid
    nextPid = nextPid+1
    ---@type process
    local processData = proc
    processData.pid = pid
    local handle = {pid=pid, result = nil, state="running"}
    processData.handle = handle
    table.insert(processes, processData)
    run_skipCurrentSleep = true
    return handle
end

---@param luaPath string
---@param argString string?
function kernel:startProcessFromPath(luaPath, argString)
    debugf("starting", luaPath)
    local f = assert(loadfile(luaPath), "failed to load file")
    local psplits = string.split(luaPath,"/")
    return kernel:startProcess({priority=0, coroutines={coroutineXPCreate(f)}, cwd=(currProcess and currProcess.cwd) or (table.concat(psplits, "/", 1, #psplits-1).."/"), resumeAfter=-1, args=argString or ""})
end

---@param processHandle processHandle
function kernel:waitForProcessExit(processHandle)
    while processHandle.state ~= "dead" do
        --print("sleep begun")
        sleep(0.1)
    end
    return processHandle.result
end

---@returns string
function kernel:getCurrentWorkingDirectory()
    return kernel:getCurrentProcess().cwd
end

---@param s string
---@return string
function kernel:normalizePath(s)
    local splitted = string.split(s, "/")
    local rv = ""
    local skipCnt = 0
    for i = #splitted, 1, -1 do
        local seg = splitted[i]
        if i > 1 and #seg == 0 then
            goto continue
        end
        if seg == ".." then
            skipCnt = skipCnt + 1
        elseif seg ~= "." then
            if skipCnt > 0 then
                skipCnt = skipCnt -1
            else
                if i == #splitted then
                    rv = seg
                else
                    rv = seg .. "/" .. rv
                end
            end
        end
        --print("seg", seg, rv)
        ::continue::
    end
    
    if skipCnt > 0 then
        return "/"
    end
    return rv
end

---@param newCwd string
function kernel:setCurrentWorkingDirectory(newCwd)
    newCwd = newCwd or "/"
    if newCwd:sub(1,1) ~= "/" then
        newCwd = "/" .. newCwd
    end
    if newCwd:sub(#newCwd,#newCwd) ~= "/" then
        newCwd = newCwd .. "/"
    end

    kernel:getCurrentProcess().cwd = self:normalizePath(newCwd)
end

return kernel