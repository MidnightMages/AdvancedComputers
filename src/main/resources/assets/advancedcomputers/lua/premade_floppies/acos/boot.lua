--local a = {}
--setmetatable(a,a)
--a[1] = 1
function _G.pp(tbl)
    for k,v in pairs(tbl) do
        print(k, ":", v)
    end
end
       
local ud = bootDrive
print("------------------KEYS------------------")
pp(vm.listUDKeys(ud))
print("----------------------------------------")
print(type(ud), typeof(ud))

_ENV._EXT.string = _ENV.string
function string.endsWith(str, suffix) return string.sub(str, #str-#suffix+1) == suffix end
function string.trimRight(str, toTrim)
    assert(#toTrim == 1, "toTrim must be exactly of length 1")
    local lastLetterToTrim = #str
    while lastLetterToTrim >= 1 do
        if str:sub(lastLetterToTrim,lastLetterToTrim) ~= toTrim then
            break
        else
            lastLetterToTrim = lastLetterToTrim - 1
        end
    end
    return str:sub(1,lastLetterToTrim)
end

function string.startsWith(str, prefix) return string.sub(str, 1, #prefix) == prefix end
---@param delim string
---@param ... string
function string.join(delim, ...) return table.concat(table.pack(...), delim) end
function string.split(str, delim, maxResultCountOrNil)
    assert(#delim == 1, "only delim len 1 supported for now")
    maxResultCountOrNil = (maxResultCountOrNil or 0)-1
    local rv = {}
    local buf = ""
    for i = 1, #str do
        local c = string.sub(str,i,i)
        if #rv ~= maxResultCountOrNil and c == delim then
            table.insert(rv, buf)
            buf = ""
        else
            buf = buf..c
        end
    end
    table.insert(rv, buf)
    return rv
end
function string.replace(str, search, replacement)
    local rv = ""
    local consumedLen = 1
    local i = 1
    while i<#str do
        if string.sub(str, i, i+#search-1) == search then
            rv = rv .. string.sub(str, consumedLen, i-1) .. replacement
            i=i+#search
            consumedLen = i
        end
        i=i+1
    end
    return rv .. string.sub(str, consumedLen)
end
function string.charCount(str, charToCount)
    local rv = 0
    assert(#charToCount == 1, "charToCount must be exactly of length 1")
    for i = 1, #str do
        if str:sub(i,i) == "charToCount" then
            rv = rv + 1
        end
    end
    return rv
end

function string.normalizeLineEndings(str) return string.replace(string.replace(str, "\r","\n"),"\r\n","\n") end

--print("this is some text")
--print(string.replace("this is some text","i","IJK"))

--print(string.startsWith("testbla","ttes"), "<-- test")



local bootDrive = _G.bootDrive
if bootDrive == nil then
    for t, a in components:list() do 
        if t == "massStorage" and a.fileExists("boot.lua") then bootDrive = a; break; end        
    end
end
assert(bootDrive ~= nil, "unable to rediscover bootdrive")

---@diagnostic disable-next-line: missing-fields
_G.package = {}
package.path = "/lib/?.lua"
package.loaded = {}
-- init filesystem
local fileHandle = bootDrive:open("/lib/filesystem.lua")
package.loaded.filesystem = assert(load(fileHandle:read(-1), "/lib/filesystem.lua")(), "failed to initialize filesystem")
fileHandle:close()
local fs = package.loaded.filesystem -- fs = require("filesystem")


function loadfile(path)
    local c = fs:readAllText(path)
    return assert(load(c, path, "t", _ENV))
end
function dofile(path, ...) return loadfile(path)(...) end

function require(moduleName)
    assert(moduleName and #moduleName > 0, "module name must be a nonempty string")
    assert(#string.split(moduleName,"/"), "module name cannot contain slashes")

    local rv = nil
    local existing = package.loaded[moduleName]
    if existing ~= nil then return existing end
    for _, p in ipairs(string.split(package.path,";")) do
        local path = string.replace(p, "?", moduleName)
        if fs:fileExists(path) then
            rv = dofile(path)
            package.loaded[moduleName] = rv
            break
        end
    end
    if rv then return rv end
    error("module '"..tostring(moduleName).."' could not be found in package.path")
end

fs = require("filesystem") -- to keep the lua plugin happy

fs:init(bootDrive)

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
                error("Boot cfg type of "..tostring(splitted[1]).." ("..currVType..") is not defined.")
            end
            error("Boot option "..tostring(splitted[1]).." was given an invalid value for expected type "..tostring(currVType).."!")
        end
        error("Boot option "..tostring(splitted[1]).." does not exist!")

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
            if chr == "\n" then print() return readInput end
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
    print(spacer.."\n"..textPad..text.."\n"..spacer)
end
if bootCfg.showLiveSystemMenu then
    
    showHeading("INSTALL-MEDIUM BOOT MENU")
    local option = selectIntegerOption("Select an option by typing the corresponding number and pressing ENTER:\n 1) Install\n 2) Boot from this medium directly")
    if option == 1 then
        showHeading("DESTINATION DISK SELECTION")
        local destMntPath = "/mnt/"
        local suffix = ""
        local nextId = 1
        local availableDisks = {}
        for t, a in components:list() do
            if a.componentType == "massStorage" then
                local desc = a.storageFamilyName.."-"..a.storageApiType.."-"..tostring(a.diskId)
                local hasOs = a:fileExists("boot.lua")
                local attribs = {}
                if a == bootDrive then table.insert(attribs, "BOOTED FROM") end
                table.insert(attribs, hasOs and "HAS OS" or "NO OS")

                desc = desc .. " ["..table.concat(attribs, ", ").."]"

                local isAllowed = a.storageApiType == "managed" and a ~= bootDrive
                suffix = suffix.."\n "..(isAllowed and tostring(nextId) or "-")..") "..desc
                if isAllowed then
                    availableDisks[nextId] = {a, hasOs}
                    nextId = nextId + 1
                end
            end
        end

        local diskOption = selectIntegerOption("Found disks are listed below. Select one to install to:\n"..suffix)

        print("Mounting...")
        fs:addMountPoint(destMntPath, availableDisks[diskOption][1])
        -- TODO clear the target filesystem before writing
        print("Copying files...")
        local blacklist = {destMntPath, "/boot.cfg"}
        fs:copyRecursive("/", destMntPath, blacklist, true)
        print("Installation complete. Press enter to exit.")
        readPrimitiveInput()
        return
    elseif option == 2 then
        -- continue
    else
        error("received out of range option: "..tostring(option))
    end
    
end

print("Loading kernel...")
local kernel = require("kernel")

-- init shell
--kernel:startProcessFromPath("/bin/lua.lua")
kernel:startProcessFromPath("/bin/sh.lua")

print("Starting kernel...")
kernel:run()

-- run autorun.lua files