---@diagnostic disable: duplicate-set-field
--[[
Helper to set up additional methods for string handling.
This also places the added function into the extension table.
]]

function string.endsWith(str, suffix)
    return string.sub(str, #str - #suffix + 1) == suffix
end

function string.trimRight(str, toTrim)
    assert(#toTrim == 1, "toTrim must be exactly of length 1")
    local lastLetterToTrim = #str
    while lastLetterToTrim >= 1 do
        if str:sub(lastLetterToTrim, lastLetterToTrim) ~= toTrim then
            break
        else
            lastLetterToTrim = lastLetterToTrim - 1
        end
    end
    return str:sub(1, lastLetterToTrim)
end

function string.startsWith(str, prefix)
    return string.sub(str, 1, #prefix) == prefix
end

function string.join(delim, ...)
    return table.concat(table.pack(...), delim)
end

function string.split(str, delim, maxResultCountOrNil)
    assert(#delim == 1, "only delim len 1 supported for now")
    maxResultCountOrNil = (maxResultCountOrNil or 0) - 1
    local rv = {}
    local buf = ""
    for i = 1, #str do
        local c = string.sub(str, i, i)
        if #rv ~= maxResultCountOrNil and c == delim then
            table.insert(rv, buf)
            buf = ""
        else
            buf = buf .. c
        end
    end
    table.insert(rv, buf)
    return rv
end

function string.replace(str, search, replacement)
    local rv = ""
    local consumedLen = 1
    local i = 1
    while i < #str do
        if string.sub(str, i, i + #search - 1) == search then
            rv = rv .. string.sub(str, consumedLen, i - 1) .. replacement
            i = i + #search
            consumedLen = i
        end
        i = i + 1
    end
    return rv .. string.sub(str, consumedLen)
end

function string.normalizeLineEndings(str)
    return string.replace(string.replace(str, "\r", "\n"), "\r\n", "\n")
end

-- fill extension table (which is separate to _ENV.string)
do
    local base = string
    local ext = _EXT.string
    ext.endsWith = base.endsWith
    ext.trimRight = base.trimRight
    ext.startsWith = base.startsWith
    ext.join = base.join
    ext.split = base.split
    ext.replace = base.replace
    ext.normalizeLineEndings = base.normalizeLineEndings
end
