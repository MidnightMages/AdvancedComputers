---@diagnostic disable: duplicate-set-field
--[[
Default BOOTLOADER implementation of AdvancedComputers
This is the entry point of execution of AdvancedOS

Expected setup from UEFI:
- print
- printInline
- shutdown
- bootDrive -- TODO take as ARGUMENT
]]


-- kernel utils table
kutils = {}

replaced = {}

local function replaceGlobalFunc(toReplace, newFunc)
    if type(toReplace) ~= "string" or replaced[toReplace] ~= nil then
        error("global function replacement: cannot replace "..tostring(toReplace))
    end
    replaced[toReplace] = _ENV[toReplace]
    _ENV[toReplace] = newFunc
end


-- Bootstrap the file system and initialize "require"
print("setting up require and loaddriver ...")

---@diagnostic disable-next-line: missing-fields
_ENV.package = {}
package.preload = {}
package.loaded = {}

package.config = "/\n:\n?\n!\n-"
package.path ="/lib/?.lua:/lib/?/init.lua"
-- package.path ="/lib/?.lua:/lib/?/init.lua:./?.lua:./?/init.lua" TODO fix local paths
-- TODO make findDriveAndDrivePath work with paths starting with ./, by taking current_process.currentWorkingDirectory into account

local bootDrive = _ENV.bootDrive
assert(bootDrive, "BOOTLOADER: undefined boot drive")

-- init modules before we have a filesystem set up
local fs = nil
do
    local function primitiveFileReadAndLoad(path)
        local handle = bootDrive:open(path)
        local func = assert(load(handle:read(-1),path), "failed to initialize primitive "..tostring(path))
        handle:close()
        return func
    end

    local function primitiveModuleLoad(moduleName, path)
        package.loaded[moduleName] = assert(primitiveFileReadAndLoad(path)(), "failed to initialize primitive module"..tostring(path))
        print("primitive module "..tostring(moduleName).." is loaded")
        return package.loaded[moduleName] -- corresponds to require("MODULE")
    end
    print("setting up primitive modules ...")
    primitiveFileReadAndLoad("/sys/stringhelpers.lua")()
    fs = primitiveModuleLoad("filesystem", "/sys/drivers/filesystem.lua") -- corresponds to fs = require("filesystem")
    package.preload.filesystem = fs
end


function loadfile(path)
    local c = fs:readAllText(path)
    return assert(load(c, path, "t", _ENV))
end

function dofile(path, ...)
    return loadfile(path)(...)
end

function package.searchpath(name, path, sep, rep)
    local conf = string.split(package.config, "\n")
    assert(type(name) == "string", "bad argument #1 to 'searchpath' (string expected, got "..type(name)..")")
    assert(type(path) == "string", "bad argument #2 to 'searchpath' (string expected, got "..type(path)..")")
    if sep == nil then
        sep = "."
    end
    assert(type(sep) == "string", "bad argument #3 to 'searchpath' (string expected, got "..type(sep)..")")
    if rep == nil then
        rep = conf[2]
    end
    assert(type(rep) == "string", "bad argument #4 to 'searchpath' (string expected, got "..type(rep)..")")
    local substitutionPoint = conf[3]
    name:replace(sep, rep)
    local tried = {}
    for _, template in ipairs(path:split(rep)) do
        if template ~= "" then
            local attempt = template:replace(substitutionPoint, name)
            if fs:fileExists(attempt) then
                return attempt
            end
            tried:insert(attempt)
        end
    end
    return nil, tried
end

package.searchers = {
    function(moduleName, usrEnv)
        return usrEnv.package.preload[moduleName]
    end,
    function(moduleName, usrEnv)
        local target = usrEnv.package.searchpath(moduleName, usrEnv.package.path)
        if target == nil then
            return
        end
        local function loader(moduleName, fileName, privileged, usrEnv)
            print(package.preload.fs)
            local fs = assert(usrEnv.package.loaded.filesystem or usrEnv.package.preload.filesystem,
                    "require requires access to the file system through package.loaded or package.preload")
            assert(privileged or not fileName:startsWith("/sys/"))
            return load(fs:readAllText(fileName), moduleName, "t", usrEnv)()
        end
        return loader, target
    end,
}

function require(modname, privileged, usrEnv)
    if usrEnv == nil then
        usrEnv = _ENV
    end
    local pkg = usrEnv.package.loaded[modname]
    if pkg ~= nil then
        return pkg
    end
    for i = 1, #usrEnv.package.searchers do
        local loader, ldrData = usrEnv.package.searchers[i](modname, usrEnv)
        if loader ~= nil then
            pkg = loader(modname, ldrData, privileged, usrEnv)
            print("loaded", modname, pkg)
            if pkg == nil then
                usrEnv.package.loaded[modname] = true
                return true
            else
                usrEnv.package.loaded[modname] = pkg
                return pkg
            end
        end
    end
    error("could not find module " .. modname)
end

fs:init(bootDrive)
-- filesystem and require done


print("initializing kernel utils ...")
-- component wrapping helpers
local WRAPPING_OBJ_KEY = {}
replaceGlobalFunc("next", function(tbl, key)
    local k, v = replaced.next(tbl, key)
    if k == WRAPPING_OBJ_KEY then
        return next(tbl, key)
    else
        return k, v
    end
end)
replaceGlobalFunc("pairs", function (tbl)
    local nxt, t, k = replaced.pairs(tbl)
    return nxt == replaced.next and next or nxt, t, k
end)

local createNextWithAllowedOnWrapped
do
    local function getNextFromWrappedWithAllowed(allowed, tbl, key)
        local target = rawget(tbl, WRAPPING_OBJ_KEY)
        local k, v = next(target, key)
        if k == nil or allowed[k] and tbl[k] == nil then
            return k, v
        else
            return getNextFromWrappedWithAllowed(allowed, tbl, key)
        end
    end

    createNextWithAllowedOnWrapped = function (allowed)
        return function (tbl, key)
            local wrapped = rawget(tbl, WRAPPING_OBJ_KEY)
            if not wrapped then
                error("this 'next' function is not applicable to the given table")
            end
            local k, v = next(tbl, key)
            if k == nil then
                -- fall back to wrapped table
                local prevK = nil
                if tbl[key] == nil then
                    -- we were already in the wrapped table
                    prevK = key
                end
                return getNextFromWrappedWithAllowed(allowed, tbl, prevK)
            end
        end
    end
end

function kutils.wrapObjectWithAccess(obj, allowedReads, allowedWrites)
    if allowedReads == nil then
        allowedReads = {}
    end
    if allowedWrites == nil then
        allowedWrites = {}
    end
    local customNext = createNextWithAllowedOnWrapped(allowedReads)
    return setmetatable({
        [WRAPPING_OBJ_KEY] = obj
    }, {
        __pairs = function (t)
            return customNext, t, nil
        end,
        __index = function (t, k)
            if allowedReads[k] then
                return obj[k]
            else
                error("Attempted to access unavailable key '"..tostring(k).."'!")
            end
        end,
        __newindex = function (t, k, v)
            if allowedReads[k] then
                if allowedWrites[k] then
                    obj[k] = v
                else
                    error("the property '"..tostring(k).."' is readonly")
                end
            else
                rawset(t, k, v)
            end
        end
    })
end

local metaPrototypes = {}
local noProtoMeta = {
    __metatable = false,
    __pairs = false,
    __newindex = function (t, k, v)
        error("setting properties for components is not allowed")
    end
}

function kutils.wrapObject(obj, prototype)
    local meta
    if prototype == nil then
        meta = noProtoMeta
    elseif metaPrototypes[prototype] ~= nil then
        meta = metaPrototypes[prototype]
    else
        meta = {
            __metatable = false,
            __pairs = false,
            __index = prototype,
            __newindex = function (t, k, v)
                error("setting properties for components is not allowed")
            end
        }
        metaPrototypes[prototype] = meta
    end
    return setmetatable({[WRAPPING_OBJ_KEY] = obj}, meta)
end

function kutils.unwrapObject(wrapped, reqType)
    local obj = rawget(wrapped, WRAPPING_OBJ_KEY)
    if reqType ~= nil and obj.componentType ~= reqType then
        error("incorrect driver selected for component")
    end
    return obj
end

function kutils.assertType(obj, tname)
    if type(obj) ~= tname then
        error("type error: expected "..tname..", got "..type(obj))
    end
    return obj
end




print("setting up proccess handling ...")




--[[
user YIELD engineering thoughts:
 - could be a syscall
 - could return to outer coroutine
 - could wait for time / hwevent / other process unblock (basically thread.join for processes or wait/notifyAll)
 - MUST check if it may actually yield (not possible if user code is run in kernel/driver context (e.g. unblock or other predicate))
]]

--[[
user RESUME engineering thoughts:
 - is actually a true yield with return to thread loop blocking THIS tread and unblocking resuming thread
 - MUST check if possible (i.e. outside of kernel/driver)
]]

--[[
scheduler:
 - version 0.1 will be a simple round robin event first proc scheduler
 - event queue which distributes events to all subscribed processes
]]

--[[
primitives:
 - require
 - stdio
 - process communication (pipes, in, out)
]]

--[[
users:
 - permissions per user
 - processes per user
 - init process for user (i.e. one shell that kills the user if closed)
 - /etc/passwd
]]





print("setting up kernel infrastructure ...")







-- maybe install OS or run live system

local bootCfg = { -- default options
    showLiveSystemMenu = false
}

if fs:fileExists("/boot.cfg") then
    local bootCfgText = string.normalizeLineEndings(fs:readAllText("/boot.cfg"))
    local bootCfgLines = string.split(bootCfgText, "\n")
    for i = 1, #bootCfgLines do
        local splitted = string.split(bootCfgLines[i], "=")
        local currV = bootCfg[splitted[1]]
        local newVStr = splitted[2]
        if currV ~= nil then -- if option exists
            local currVType = type(currV)
            if currVType == "boolean" then
                if newVStr == "true" or newVStr == "false" then
                    bootCfg[splitted[1]] = newVStr == "true"
                    goto continue
                end
            else
                error("Boot cfg type of " .. tostring(splitted[1]) .. " (" .. currVType .. ") is not defined.")
            end
            error("Boot option " ..
                tostring(splitted[1]) .. " was given an invalid value for expected type " .. tostring(currVType) .. "!")
        end
        error("Boot option " .. tostring(splitted[1]) .. " does not exist!")

        ::continue::
    end
end

local function readPrimitiveInput()
    local readInput = ""
    while true do
        local nextEvent = table.pack(components:getFirst("computer"):getMachineEvent())
        if nextEvent[1] == nil then
            sleep(0.1)
        elseif nextEvent[1] == "charTyped" then
            local chr = nextEvent[2]
            if chr == "\n" then
                print()
                return readInput
            end
            if chr == "\b" then
                if #readInput > 0 then
                    readInput = string.sub(readInput, 1, -2)
                    printInline(chr)
                end
            else
                printInline(chr)
                readInput = readInput .. chr
            end
        elseif nextEvent[1] == "shutdown" then
            error("shutdown requested")
        end
    end
end

local function selectIntegerOption(title)
    print(title)
    return assert(tonumber(readPrimitiveInput()), "invalid option given")
end

local function showHeading(text, spacerChar)
    spacerChar = spacerChar or "="
    local spacer = string.rep("=", #text + 4)
    local textPad = string.rep(" ", 2)
    print(spacer .. "\n" .. textPad .. text .. "\n" .. spacer)
end
if bootCfg.showLiveSystemMenu then
    showHeading("INSTALL-MEDIUM BOOT MENU")
    local option = selectIntegerOption(
        "Select an option by typing the corresponding number and pressing ENTER:\n 1) Install\n 2) Boot from this medium directly")
    if option == 1 then
        showHeading("DESTINATION DISK SELECTION")
        local destMntPath = "/mnt/"
        local suffix = ""
        local nextId = 1
        local availableDisks = {}
        for t, a in components:list() do
            if a.componentType == "massStorage" then
                local desc = a.storageFamilyName .. "-" .. a.storageApiType .. "-" .. tostring(a.diskId)
                local hasOs = a:fileExists("boot.lua")
                local attribs = {}
                if a == bootDrive then table.insert(attribs, "BOOTED FROM") end
                table.insert(attribs, hasOs and "HAS OS" or "NO OS")

                desc = desc .. " [" .. table.concat(attribs, ", ") .. "]"

                local isAllowed = a.storageApiType == "managed" and a ~= bootDrive
                suffix = suffix .. "\n " .. (isAllowed and tostring(nextId) or "-") .. ") " .. desc
                if isAllowed then
                    availableDisks[nextId] = { a, hasOs }
                    nextId = nextId + 1
                end
            end
        end

        local diskOption = selectIntegerOption("Found disks are listed below. Select one to install to:\n" .. suffix)

        print("Mounting...")
        fs:addMountPoint(destMntPath, availableDisks[diskOption][1])
        -- TODO clear the target filesystem before writing
        print("Copying files...")
        local blacklist = { destMntPath, "/boot.cfg" }
        fs:copyRecursive("/", destMntPath, blacklist, true)
        print("Installation complete. Press enter to exit.")
        readPrimitiveInput()
        return
    elseif option == 2 then
        -- continue
    else
        error("received out of range option: " .. tostring(option))
    end
end

-- live system stuff done





-- init

print("Starting kernel...")
dofile("/sys/kernel.lua")
print("kernel has exited")


-- run autorun.lua files
