local ap = {}


local escSequenceMap = {
    ["\\"] = "\\",
    ["0"] = "\0",
    ["r"] = "\r",
    ["n"] = "\n",
    ['"'] = '"',
    ["'"] = "'",
}
function ap:tryExtractQuotedSegments(...)
    local str = string.join(" ", ...)
    local rv = {}

    local quotetype = nil
    local buf = ""
    local lastC = nil
    local c = nil
    for i = 1, #str do
        lastC = c
        c = str:sub(i,i)

        if lastC == "\\" then
            local repl = escSequenceMap[c]
            if not repl then return false, "Invalid escape sequence \\"..c end
            buf = buf..repl
        else
            if c == quotetype then -- end of quoted string
                table.insert(rv, buf)
            elseif quotetype == nil and (c == '"' or c == '"') then -- start of quoted string
                quotetype = c
            elseif c == " " then -- arg split
                table.insert(rv, buf)
                buf = ""
            else -- normal letter
                buf = buf..c
            end
        end
    end

    if quotetype then return false, "Unterminated string" end
    return true, rv
end

function ap:extractQuotedSegments(...) 
    local ok, rv = ap:tryExtractQuotedSegments(...) 
    if ok then return rv else error(rv) end
end

return ap