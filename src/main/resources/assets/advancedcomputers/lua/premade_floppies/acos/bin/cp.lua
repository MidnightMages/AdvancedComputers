--[[
copying utility
]]

local argParser = require("argparse").parser("cp")
    :optional("f", "override an existing <destfile>")
    :positional("sourcefile", "file to copy")
    :positional("destfile", "path to copy the file to")
    :endRequired()

local ok, opt, pos = argParser:parse(...)
if not ok then
    print(opt)
    return -1
end

local fs = require "filesystem"

if not fs:fileExists(pos.sourcefile) then
    print("source file does not exist")
    return -2
end

if fs:fileExists(pos.destfile) and not opt.f then
    print("use option '-f' to overwrite an existing file")
    return -3
end

fs:writeAllText(pos.destfile, fs:readAllText(pos.sourcefile))
print("copied " .. pos.sourcefile .. " to " .. pos.destfile)
