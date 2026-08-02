--[[

]]
local driverAccessibleData = ...

local assertPermission = kutils.assertPermission
local wrap = kutils.wrapObjectWithAccess
local unwrap = kutils.unwrapObject
local scheduler = scheduler

local DRIVER_NAME <const> = "process"
local PERMISSION <const> = "drv.process"
kutils.registerPermission(PERMISSION)


local PROCESS <const> = {
    curId = -1
}

local ALLOWED_READS = {
    parentProcess = true,
    id = true,
    description = true,
    currentWorkingDirectory = true,
    args = true,
    state = true,
    endedSuccessfully = true
}

---@class OsThread
---@field id integer
---@field pausedUntil integer
---@field queuedEvents any[][]
---@field coroutine thread


---@class ProcessStartInfo
---@field description string
---@field currentWorkingDirectory string

---@class FullProcessStartInfo : ProcessStartInfo
---@field mainFunc function
---@field args any[]



---@class Process : ProcessStartInfo
---@field id integer
---@field state PROCESS_RUNSTATE
---@field endedSuccessfully boolean

---@class ProcessWithPrivateFields : Process
---@field parentProc Process
---@field unblockedThreads OsThread[]
---@field blockedThreads OsThread[]
---@field createThread function
---@field nextThreadId integer
---@field textBuffers TextBuffer[]
---@field activeTextBuffers TextBuffer[]

---@enum PROCESS_RUNSTATE
PROCESS_RUNSTATE = {
    unstarted = 0,
    runnable = 1,
    running = 2,
    dead = 3
}

---@param processStartInfo FullProcessStartInfo
---@return ProcessWithPrivateFields
function PROCESS.new(processStartInfo, parentProcess)
    PROCESS.curId = PROCESS.curId + 1

    assert(getmetatable(processStartInfo) == nil, "processStartInfo cannot have a metatable attached")
    local desc = processStartInfo.description
    local cwd = processStartInfo.currentWorkingDirectory
    local args = { table.unpack(processStartInfo.args) }

    local proc = setmetatable({
        -- public
        parentProcess = parentProcess,
        description = desc,
        currentWorkingDirectory = cwd,

        id = PROCESS.curId,
        -- private
        textBuffers = {},       -- text buffers associated with this process
        activeTextBuffers = {}, -- no active buffer for a given screen means it just shows the parent process
        unblockedThreads = {},  -- all os-threads that are currently resumable
        blockedThreads = {},    -- all os-threads that are currently not resumable
        nextThreadId = 0,
        state = PROCESS_RUNSTATE.running,
        freeAllBuffers = function(self)
            for i = #self.textBuffers, 1, -1 do
                local b = textBuffers[i]
                if b.isAlive then
                    b:free()
                end
            end
        end
    }, {
        __index = PROCESS
    })
    proc:createThread(processStartInfo.mainFunc, args)
    return proc
end

function PROCESS:createThread(funcToExecute, packedArgs)
    local tid = self.nextThreadId
    self.nextThreadId = tid + 1

    ---@type OsThread
    local newThread = {
        coroutine = coroutine.create(function()
            funcToExecute(table.unpack(packedArgs))
        end),
        id = tid,
        pausedUntil = -1,
        queuedEvents = {}
    }
    table.insert(self.unblockedThreads, newThread)
end

local syscalls = {}


-- syscalls


---@param processStartInfo FullProcessStartInfo
function syscalls.spawnProcess(processStartInfo) -- TODO chose isolation level and thus prototype-_ENV, i.e 0 = kernel, 1 = driver, 2 = user?
    local proc = PROCESS.new(processStartInfo, kutils.getCurrentProcess())
    scheduler:enqueue(proc)
    print("created", proc.description)
    local rv = wrap(proc, ALLOWED_READS)
    print("created rv", rv.description)
    return rv
end

return syscalls
