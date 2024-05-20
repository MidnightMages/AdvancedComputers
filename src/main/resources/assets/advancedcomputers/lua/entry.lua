-- sandbox setup
print("Setting up LUA sandbox")

local _setStopCode
local _luaShellCode

local function init()
    local oldPrint = _G["print"]
    _G["print"] = function(...)
        local args = table.pack(...)
        for i=1,#args do
            args[i] = tostring(args[i])
        end
        oldPrint(table.unpack(args))
    end

    local oldDebug = _G["debug"]
    _G["debug"] = {
        ["traceback"] = oldDebug.traceback
    }

    local oldLoad = _G["load"]
    _G["load"] = function(chunk, chunkname, mode, env) -- based on http://lua-users.org/wiki/SandBoxes
        if type(chunk) ~= "string" then error("Expected first argument to be of type string.") end
        if chunk:byte(1) == 27 then error("Loading LUA bytecode is not allowed.") end
        if mode ~= "t" then error("Specifying a load-mode (third argument) other than 't' is forbidden.") end
        return oldLoad(chunk, chunkname, "t", env) -- returns function on success, else (nil,errorMessage)
    end

    setEventCallback(function(...)
        print("CALLBACK: Received event with args:", ...)
    end)

    _G["setEventCallback"] = nil

    local function GetAndClearGlobal(name)
        local copy = _G[name]
        if copy == nil then error("A global with name " .. tostring(name) .. " was nil!") end
        _G[name] = nil
        return copy
    end

    local sandboxCountHookCallback = GetAndClearGlobal("sandboxCountHookCallback")
    local sandboxCountHookCallbackInterval = GetAndClearGlobal("sandboxCountHookCallbackInterval")
    _setStopCode = GetAndClearGlobal("setStopCode")
    oldDebug.sethook(sandboxCountHookCallback, "c", sandboxCountHookCallbackInterval)


    --[[
    TODO sanitize

    collectgarbage
    metatables?
    ]]--

    -- build 'computer' table
    local computer = {};
    computer["getMachineEvent"] = GetAndClearGlobal("getMachineEvent")
    computer["waitForMachineEvent"] = GetAndClearGlobal("waitForMachineEvent")
    _G["computer"] = computer;

    _luaShellCode = GetAndClearGlobal("luaShell")

    local function arrayContains(t, obj)
        for i=1,#t do
            if t[i] == obj then return true end
        end
        return false
    end

    print("Init begun")
    local whitelistedGlobals = {
        "_G", "setmetatable", "warn", "tonumber", "table", "rawequal", "select", "load", "pcall", "string", "math",
        "rawlen", "print", "_VERSION", "xpcall", "error","assert", "tostring","getmetatable", "pairs",
        "rawset","rawget","ipairs","next","type","collectgarbage", "coroutine",

        -- already sanitized
        "debug",

        -- custom objects
        "computer", "clear", "printInline", "sleep"
    }

    for k,v in pairs(_G) do
        local name = tostring(k)
         if not arrayContains(whitelistedGlobals, name) then
             _G[name] = nil
             print("Removed "..name)
        end
        print(tostring(k),":",tostring(_G[k]))
    end

    print("init ended")
end

print("Testing!")
local ok, rv = pcall(init)
if not ok then
    print("SANDBOX ERROR:", rv)
    _setStopCode("SANDBOX ERROR:", rv)
    error(rv)
end

local _luaShell, err = _G.load(_luaShellCode, "luaShell", "t", _G)
print(_luaShell)
if _luaShell == nil then
    error("shell compilation error: " .. err)
end
print("starting shell")
ok, rv = xpcall(_luaShell, debug.traceback)
if not ok then
    print("ERROR:", rv)
    _setStopCode("ERROR:", rv)
    error(rv)
end

print("")
printInline("\nshutting down .")
sleep(0.5)
printInline(" .")
sleep(0.5)
printInline(" .")
sleep(0.5)
printInline("\nBYE")
sleep(0.75)
_setStopCode("")
