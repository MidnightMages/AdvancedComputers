--[[
copying utility
]]

local argParser = require("argparse").parser("cp")
    :optional("f", "override an existing <destfile>")
    :positional("sourcefile", "file to copy")
    :positional("destfile", "path to copy the file to")

local source
local dest
local force
do
    local ok, optionals_err, positionals = argParser:parse(...)
    if not ok then
        print(optionals_err)
        return -1
    end
    source = positionals.sourcefile
    if not source then
        print("Argument error: missing source")
        print(argParser:help())
        return -1
    end
    dest = positionals.destfile
    if not dest then
        print("Argument error: missing destination")
        print(argParser:help())
        return -1
    end
    force = optionals_err.f
end

local fs = require "filesystem"

if not fs:fileExists(source) then
    print("source file does not exist")
    return -2
end

if fs:fileExists(dest) and not force then
    print("use option '-f' to overwrite an existing file")
    return -3
end

fs:writeAllText(dest, fs:readAllText(source))
print("copied " .. source .. " to " .. dest)
