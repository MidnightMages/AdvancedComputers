--[[
Default UEFI implementation of AdvancedComputers
This is the entry point of execution

If NV-RAM is available, we try accessing the following options with their respective defaults:
- uefi_bootOptionsSleepTime = 3 [sleep time before taking default boot option in seconds]
- uefi_stackTraceOnCrash = false [if a stack trace is printed upon crash of OS]
- uefi_bootDrive = "" [path to drive to boot from] !!! NOT IMPLEMENTED YET !!!

Assumed _ENV on boot:
_ENV = {
    ... builtins ...,
    components,
    print
    printInline
    sleep
    setReboot
}
]]
print("heartbeat")

local SLEEP_TIME <const> = 3


-- initialize printing
local gpu = components:getFirst("gpu")
-- we assume each GPU to at least provide space for 110x44 characters
local screenSizeX, screenSizeY = 110, 44
local globalStdOutBuffer = gpu:newBuffer(screenSizeX, screenSizeY)
for t, v in components:list() do
    if t == "screen" then
        gpu:assignBuffer(globalStdOutBuffer, v)
    end
end

local cursorX, cursorY = 0, 0 -- to be captured by new prints
local function makePrint(target, append)
    local function doPrint(...) -- new printing to screen
        local packed = table.pack(...)
        for i = 1, #packed do
            if packed[i] == nil then packed[i] = "nil" end
        end
        cursorX, cursorY = globalStdOutBuffer:pasteText(
            cursorX, cursorY, "SCROLL_SPILL_CLEAR",
            table.concat(packed, " ") .. append)
    end
    local oldPrint <const> = _ENV[target]
    if oldPrint ~= nil then -- possibly prepend console print
        _ENV[target] = function(...)
            oldPrint(...)
            doPrint(...)
        end
    else
        _ENV[target] = doPrint
    end
end
makePrint("print", "\n")
makePrint("printInline", "")
print("booting UEFI ...\nprinting initialized")
_ENV.uefiTextBuffer = globalStdOutBuffer -- inform OS about our text buffer


-- check NV-RAM and possibly load config
local bootOptionsSleepTime
local stackTraceOnCrash
local computer = components:getFirst("computer")
if computer.nvram ~= nil then
    print("nvram is available")
    bootOptionsSleepTime = computer.nvram.uefi_bootOptionsSleepTime
    stackTraceOnCrash = computer.nvram.uefi_stackTraceOnCrash
else
    print("nvram is unavailable")
end

stackTraceOnCrash = true

bootOptionsSleepTime = bootOptionsSleepTime or SLEEP_TIME
stackTraceOnCrash = not not stackTraceOnCrash


-- init boot function
local function bootFromMedium(medium)
    _ENV.bootDrive = medium -- set boot drive
    local bootHandle = medium:open("boot.lua")
    local code = bootHandle:read(-1)
    bootHandle:close()
    print("loading boot.lua ...")
    local bootFunc, errMsg = load(code, "boot.lua")
    if not bootFunc then
        print("ERROR loading boot.lua!\nexiting ...")
        sleep(SLEEP_TIME)
        return
    end
    -- save stuff we want to use after running the bootloader
    print("initialize exit handling ...")
    local saved <const> = {
        setReboot = setReboot,
        yield = coroutine.yield,
        print = print,
        pack = table.pack,
        unpack = table.unpack,
    }
    setReboot = nil -- we do not make that available to guest OS
    function shutdown(isReboot)
        saved.yield("shutdown", isReboot)
    end

    if stackTraceOnCrash then
        local realBootFunc = bootFunc
        bootFunc = function()
            local result = saved.pack(xpcall(realBootFunc, debug.traceback))
            if result[1] then
                return saved.unpack(result, 2, result.n) -- spread results
            else
                error(result[2], 0)                 -- propagate error
            end
        end
    end
    -- let's boot for real
    print("booting ...")
    local result = saved.pack(coroutine.resume(coroutine.create(bootFunc)))
    print ("post echo")
    if not result[1] then
        -- some error has occurred, try printing error
        local errMsg = tostring(result[2])
        pcall(saved.print, "UEFI: OS has crashed:\n" .. errMsg)
        error("OS ERROR: " .. errMsg)
        return
    else
        -- successful exit
        if result[2] == "shutdown" then
            saved.setReboot(result[3])
        end
    end
end


-- find bootable media
print("\navailable components:")
local defaultBoot
local idx = 1
local bootables = {}
local bootableCount = 0
for compType, elem in components:list() do
    print("- " .. compType)
    if compType == "massStorage" then
        if elem:fileExists("boot.lua") then
            -- we found a bootable medium, use it as default if not already set
            defaultBoot = defaultBoot or idx
            bootables[idx] = elem
			bootableCount = bootableCount + 1
        end
        idx = idx + 1
    end
end

-- boot options
if bootableCount < 1 then
    print("no bootable medium found!\nexiting ...")
    sleep(SLEEP_TIME)
    return
elseif bootableCount == 1 then
    local bootTarget = bootables[defaultBoot]
    print("booting from medium-" .. defaultBoot .. "-" .. bootTarget.storageFamilyName .. " ...")
    bootFromMedium(bootTarget)
else
    print("\nboot options (default is top)")
    for idx, medium in pairs(bootables) do
        print(tostring(idx)..": medium-" .. idx .. "-" .. medium.storageFamilyName)
    end
    local bootTarget = defaultBoot
    local remaining = bootOptionsSleepTime * 10
    while true do
        printInline("enter boot medium id: ") -- only single char allowed
        while true do
            local nextEvent = table.pack(computer:getMachineEvent())
            if nextEvent[1] == "charTyped" then
                local requested = nextEvent[2]
				local requestedNumber = tonumber(requested)
                if requestedNumber ~= nil and bootables[requestedNumber] ~= nil then
                    bootTarget = bootables[requestedNumber]
                    print("booting medium-" .. requested .. "-" .. bootTarget.storageFamilyName)
                    goto bootingLabel
                else
                    print("invalid medium id")
                    remaining = -1 -- disable default timeout
                    break
                end
            end
            if remaining == 0 then -- ignore negatives
                print("choosing default ...")
                goto bootingLabel
            end
            remaining = remaining - 1
            sleep(0.1)
        end
    end
    ::bootingLabel::
    bootFromMedium(bootTarget)
end
