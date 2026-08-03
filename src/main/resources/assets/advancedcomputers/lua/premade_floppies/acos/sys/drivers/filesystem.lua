local fs = {}
-- TODO create file containing fs metadata, listing ids of disks and mountpoints, 
-- such that it is only necessary for the boot drive to be in a predictable slot or specified by the bios, but the others can be in any order
local mounts = {}

local function normalizePath(path)
    -- TODO add process's working directory if the path odes not start with /
    if not string.startsWith(path, "/") then
        -- prepend current working dir
        path = kutils.getCurrentProcess().currentWorkingDirectory .. path
    end
    local segments = string.split(path,"/")
    --print("splitres:",#segments, segments[1]..";", segments[2]..";")
    local skipCnt = 0
    local output = ""
    for i = #segments, 1, -1 do
        local seg = segments[i]
        --print("seg:",seg)
        if seg == ".." then
            skipCnt = skipCnt + 1
        elseif seg ~= "." then
            if skipCnt > 0 then
                skipCnt = skipCnt - 1
            else
                if #output > 0 then
                    seg = seg.."/"
                end
                output = seg..output
            end
        end
    end
    return output
end

---comment
---@param path any
---@param drive userdata
function fs:addMountPoint(path, drive)
    if not string.endsWith(path, "/") then
        path = path .. "/"
    end
    assert(drive, "no drive specified")
    print("Adding mount: '"..tostring(path).."'")
    assert(not mounts[path], "mount point '"..path.."' already exists")
    mounts[path] = drive
end

function fs:getMountPointTable()
    return {table.unpack(mounts)}
end

local function getMountPoint(path)
    local currPrefix = "/"
    local currDrive = mounts[currPrefix]
    local currLen = #currPrefix
    if not string.endsWith(path,"/") then path = path .. "/" end
    --print("looking for mountpoint of", path)
    for prefix, drive in pairs(mounts) do
        --print("checking", prefix, drive)
        if #prefix > currLen and string.startsWith(path, prefix) then
            currDrive, currPrefix = drive, prefix
        end
    end
    return currDrive, currPrefix
end

local function findDriveAndDrivePath(filePath)
    local p = normalizePath(filePath)
    local drive, prefix = getMountPoint(p)
    local drivePath = "/"..string.sub(p, #prefix+1)
    return drive, drivePath
end

function fs:readAllText(filePath)
    local drive, drivePath = findDriveAndDrivePath(filePath)
    local fileHandle = drive:open(drivePath)
    local rv = fileHandle:read(-1) or "" -- if the file is empty we get nil (end of line), in that case just return an empty string
    fileHandle:close()
    return rv
end

function fs:writeAllText(filePath, content)
    local drive, drivePath = findDriveAndDrivePath(filePath)
    local handle = drive:open(drivePath, "w")
    handle:write(content)
    handle:close()
end

function fs:appendAllText(filePath, content)
    local drive, drivePath = findDriveAndDrivePath(filePath)
    local handle = drive:open(drivePath, "a")
    handle:write(content)
    handle:close()
end

function fs:fileExists(filePath)
    local drive, drivePath = findDriveAndDrivePath(filePath)
    return drive:fileExists(drivePath)
end

function fs:directoryExists(filePath)
    local drive, drivePath = findDriveAndDrivePath(filePath)
    return drive:directoryExists(drivePath)
end

function fs:makeDirectory(filePath)
    local drive, drivePath = findDriveAndDrivePath(filePath)
    drive:makeDirectory(drivePath)
end

function fs:deleteDirectory(filePath)
    local drive, drivePath = findDriveAndDrivePath(filePath)
    drive:deleteDirectory(drivePath)
end

function fs:delete(filePath)
    local drive, drivePath = findDriveAndDrivePath(filePath)
    drive:delete(drivePath)
end

function fs:list(filePath)
    local drive, drivePath = findDriveAndDrivePath(filePath)
    local res = drive:list(drivePath)
    if not string.endsWith(filePath, "/") then filePath = filePath + "/" end
    for prefix, _ in pairs(mounts) do
        --print("checking", prefix, filePath, #string.split(prefix:sub(#filePath+1),"/"), prefix:sub(#filePath+1), string.charCount(prefix:sub(#filePath+1), "/"))
        if string.startsWith(prefix, filePath) then -- and #string.split(prefix:sub(#filePath+1),"/") == 1
            local innerFolder = string.trimRight(prefix:sub(#filePath+1),"/")
            --print("inner folder:",innerFolder)
            if #innerFolder > 0 then
                table.insert(res, innerFolder .. "/")
            end
        end
    end
    --print(res)
    return res
end

local function tableContains(t, value)
    for index, v in ipairs(t) do
        if value == v then
            return true
        end
    end
    return false
end

function fs:copyRecursive(srcPath, destPath, blacklistOrNil, verboseOrNil)
    if blacklistOrNil and tableContains(blacklistOrNil, srcPath) then return end
    if verboseOrNil then print("Copying: ".. srcPath .." --> "..destPath) end
    if not string.endsWith(srcPath, "/") then -- if file
        fs:writeAllText(destPath, fs:readAllText(srcPath))
        return
    end

    assert(fs:directoryExists(srcPath), "source directory "..tostring(srcPath).." does not exist.")
    fs:makeDirectory(destPath)

    -- if directory
    for _, f in ipairs(fs:list(srcPath)) do
        fs:copyRecursive(srcPath .. f, destPath .. f, blacklistOrNil, verboseOrNil)
    end
end

function fs:init(bootDrive)
    assert(bootDrive)
    fs:addMountPoint("/", bootDrive)
    -- load other mountpoints from fstab
    local fst = "/etc/fstab"
    if not fs:fileExists(fst) then
        fs:writeAllText(fst, "")
    else
        local t = fs:readAllText(fst)
        for _, v in ipairs(string.split(t,"\n")) do
            if #v>0 then
                local s = string.split(v, "=", 2)
                assert(#s == 2, "invalid mountpoint definition: "..tostring(v))
                local diskId,path = table.unpack(s)
                local validDiskIds = ""
                local found = false
                for t2, a in components:list() do
                    if t2 ~= "massStorage"  then goto continue end
                    local componentDiskId = "massStorage_"..a.diskId
                    print(t2, componentDiskId, diskId)
                    if componentDiskId == diskId then
                        fs:addMountPoint(path, a)
                        found = true
                        break
                    else
                        validDiskIds = validDiskIds .. componentDiskId .. ";"
                    end
                    ::continue::
                end
                if not found then
                    error("Unable to find storage '"..tostring(diskId).."' for mountpoint '"..tostring(path).."'! Valid drives found: "..validDiskIds)
                end
            end
        end
    end
end



return fs