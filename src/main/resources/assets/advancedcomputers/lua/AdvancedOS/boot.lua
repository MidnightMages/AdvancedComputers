local coroutineResumeOrig = _G.coroutine.resume
local coroutineCreateOrig = _G.coroutine.create

--[[
    
_G.computer = {}
_G.computer.sleep = function(duration) -- performs a non-wasteful-sleep
    
end

_G.computer.getMachineEvents = function() -- returns (string eventName, varargsPacked)[]
    return {}
end

_G.computer.waitForMachineEvent = function(timeout) -- returns after timeout or instantly if GetMachineEvents were to return a non-empty table
    
end

_G.computer.shutdown = function(reboot)
    
end

_G.computer.uptime = function() -- time since computer boot (not increasing while paused)
    
end
]]

local coroutineParentMap = {} -- Dict<coroutine coroutine, coroutine parent>
local coroutineExecutionQueue = {}
local coroutineRescheduleMap = {} -- Dict<coroutine co, number earliestRescheduleTimestamp>
local coroutineResumptionArguments = {} -- Dict<coroutine co, varargsPacked args>
local shutdownTasks = {} -- function[]
local eventQueue = {} -- (string eventName, varargsPacked)[]

local subscribedCoroutineMap = {} -- Dict<string eventname, (coroutine subscribedCoroutine)[]>
local subscribedCoroutineMap_Inverse = {} -- Dict<coroutine subscribedCoroutine, string eventname>
local function subscribedCoroutineMap_add(eventKey, newCoroutine)
    local existing = subscribedCoroutineMap[eventKey]
    if not existing then
        subscribedCoroutineMap[eventKey] = {newCoroutine}
    else
        table.insert(existing, newCoroutine)
    end
    subscribedCoroutineMap_Inverse[newCoroutine] = eventKey
end

local function subscribedCoroutineMap_removeByKey(eventKey)
    local existing = subscribedCoroutineMap[eventKey]
    for i = 1, #existing do
        subscribedCoroutineMap_Inverse[existing[i]] = nil
    end
    subscribedCoroutineMap[eventKey] = nil
end

local function subscribedCoroutineMap_removeByValue(coroutine)
    local existingTable = subscribedCoroutineMap[subscribedCoroutineMap_Inverse[coroutine]]
    for i = 1, #existingTable do
        if existingTable[i] == coroutine then
            table.remove(existingTable, i)
            break
        end
    end

    subscribedCoroutineMap_Inverse[coroutine] = nil
end

local function enqueueCoroutineWithArgs(co, packedVarArgs)
    coroutineResumptionArguments[co] = packedVarArgs
    table.insert(coroutineExecutionQueue, co)
end

local function processEvent(eventTable)
    local eventName = eventTable[1]
    local toExecute = subscribedCoroutineMap[eventName]
    if toExecute then
        for i = 1, #toExecute do
            local co = toExecute[i]
            enqueueCoroutineWithArgs(co, eventTable)
        end
        subscribedCoroutineMap_removeByKey(eventName)
    end
end

local COROUTINE_WAIT_TYPE = {}
COROUTINE_WAIT_TYPE.EVENT_OR_TIMEOUT = 1 -- {type}
COROUTINE_WAIT_TYPE.SUB_YIELDRET = 2  -- {type}
COROUTINE_WAIT_TYPE.SUPER_RESUMPTION = 3 -- {type}

local coroutineWaitReason = nil
---@diagnostic disable-next-line: duplicate-set-field
_G.coroutine.resume = function(co, ...)
    coroutineWaitReason = COROUTINE_WAIT_TYPE.SUB_YIELDRET
    _G.coroutine.yield({co, table.pack(...)})
end

---@diagnostic disable-next-line: duplicate-set-field
_G.coroutine.create = function(f)
    local co = coroutineCreateOrig(f)
---@diagnostic disable-next-line: need-check-nil
    coroutineParentMap[co] = coroutine.running()
end

---@diagnostic disable-next-line: duplicate-set-field
_G.coroutine.wrap = function(f)
    local co = _G.coroutine.create(f)
    return function(...)
        local packed = table.pack(_G.coroutine.resume(co, ...))
        if not packed[1] then
            error("Error during coroutine.wrap: "..tostring(packed[2]))
        end
        return table.unpack(packed, 2)
    end
end

-- ---@diagnostic disable-next-line: duplicate-set-field
-- _G.coroutine.yield = function(...)
-- end

_G.os = {}
_G.os.spawnCoroutine = function(func, ...) -- Creates a new coroutine that is managed by the scheduler but does not have a parent, optionally with arguments
    local co = coroutineCreateOrig(func)
    enqueueCoroutineWithArgs(co, table.pack(...))
end

_G.event = {}
event.pull = function(timeout, eventName)
    if timeout == nil and eventName == nil then
       error("At least one argument must be non-nil.")
    end

    coroutineWaitReason = COROUTINE_WAIT_TYPE.EVENT_OR_TIMEOUT
    return _G.coroutine.yield({timeout, eventName})
end

event.push = function(eventName, ...)
    table.insert(eventQueue, {eventName, table.pack(...)})
end

event.subscribe = function(eventName, func)
    local unsubscriber = {}
    if eventName == "shutdown" then
        function unsubscriber:unsubscribe()
            for i = #shutdownTasks, 1, -1 do
                if shutdownTasks[i] == func then
                    table.remove(shutdownTasks, i)
                end
            end
        end
        table.insert(shutdownTasks, func)
    else
        unsubscriber["unsubscribed"] = false
        function unsubscriber:unsubscribe() self.unsubscribed = true end

        os.spawnCoroutine(function()
            while true do
                local args = table.pack(event.pull(nil, eventName))
                if unsubscriber["unsubscribed"] ~= false then
                    break
                end
                func(table.unpack(args))
            end
        end)
    end
    return unsubscriber
end


_G.os.sleep = function(duration)
    _G.event.pull(duration, nil)
end

local shutdownOs = false
local rebootOs = nil

_G.os.shutdown = function(reboot)
    shutdownOs = true
    rebootOs = reboot or false
end

local function printSched(...)
    print("[Scheduler]",...)
end

local lastCoroutineStartTime = nil -- TODO add yield hook, and if non nil and greater than the timeout then we do something

local function coroutineScheduler()
    local i = 1
    while true do
        if shutdownOs then
            printSched("Beginning shutdown")
            for i = #shutdownTasks, 1, -1 do
                shutdownTasks[i]()
            end
            printSched("Shutdown complete")
            computer.shutdown(rebootOs)
        end

        -- check event queues
        for j = 1, #eventQueue do processEvent(eventQueue[i]) end
        local machineEvents = computer.getMachineEvents()
        for j = 1, #machineEvents do processEvent(machineEvents[i]) end

        -- process timeouts
        local now = computer.uptime()
        local removed = {}
        local earliestTimestamp = math.huge
        for k,v in pairs(coroutineRescheduleMap) do
            if v < now then
                table.insert(removed, k)
                enqueueCoroutineWithArgs(k, nil)
                subscribedCoroutineMap_removeByValue(k)
            elseif v < earliestTimestamp then
                earliestTimestamp = v
            end

        end
        for j = 1, #removed do coroutineRescheduleMap[removed[j]] = nil end

        -- efficiently sleep if there are no tasks
        if #coroutineExecutionQueue == 0 then
            local waitTime = math.max(earliestTimestamp-now,0)
            computer.waitForMachineEvent(waitTime)
            goto continue
        end

        -- process the next task
        local nextCo = table.remove(coroutineExecutionQueue, 0)
        local nextArgs = coroutineResumptionArguments[nextCo]
        coroutineResumptionArguments[nextCo] = nil
        
        coroutineWaitReason = COROUTINE_WAIT_TYPE.SUPER_RESUMPTION
        lastCoroutineStartTime = computer.uptime()
        local coReturnValue = nil
        if nextArgs ~= nil then
            coReturnValue = table.pack(coroutineResumeOrig(nextCo, table.unpack(nextArgs)))
        else
            coReturnValue = table.pack(coroutineResumeOrig(nextCo))
        end
        lastCoroutineStartTime = nil

        if coroutineWaitReason == COROUTINE_WAIT_TYPE.EVENT_OR_TIMEOUT then
            assert(coReturnValue[1] == true, "EVENT_OR_TIMEOUT failed")
            local coData = coReturnValue[2]
            local timeout = coData[1]
            local eventName = coData[2]

            if timeout ~= nil then coroutineRescheduleMap[nextCo] = computer.uptime() + timeout end
            if eventName ~= nil then subscribedCoroutineMap_add(eventName, nextCo) end
        elseif coroutineWaitReason == COROUTINE_WAIT_TYPE.SUB_YIELDRET then -- coReturnValue: {bool success, {coroutine co to resume, packedVarArgs}}
            assert(coReturnValue[1] == true, "SUB_YIELDRET failed")
            local coData = coReturnValue[2]
            enqueueCoroutineWithArgs(coData[1], coData[2])
        elseif coroutineWaitReason == COROUTINE_WAIT_TYPE.SUPER_RESUMPTION then
            local blocker = coroutineParentMap[nextCo]
            if blocker then -- coroutine was not started by the main lua coroutine
                enqueueCoroutineWithArgs(blocker, coReturnValue)
            else
                if coReturnValue[1] == false then -- if coroutine has errored and there is no parent coroutine
                    error("Coroutine errored with reason: "..tostring(coReturnValue[2]))
                end
                if coroutine.status(nextCo) == "suspended" then -- a root coroutine has yielded
                    enqueueCoroutineWithArgs(nextCo, nil)
                end -- otherwise drop
            end
        else
            error("Not implemented")
        end
        ::continue::
    end
    error("All coroutines have exited. Shutdown time?")
end


local function boot()

end

os.spawnCoroutine(boot)
coroutineScheduler()