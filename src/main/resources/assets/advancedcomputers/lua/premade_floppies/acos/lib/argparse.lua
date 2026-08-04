local ap = {}


local escSequenceMap = {
    ["\\"] = "\\",
    ["0"] = "\0",
    ["r"] = "\r",
    ["n"] = "\n",
    ['"'] = '"',
    ["'"] = "'",
}

function ap.tryExtractQuotedSegments(...)
    local str = string.join(" ", ...)
    local rv = {}

    local quotetype = nil
    local buf = ""
    local lastC = nil
    local c = nil
    for i = 1, #str do
        lastC = c
        c = str:sub(i, i)

        if lastC == "\\" then
            local repl = escSequenceMap[c]
            if not repl then return false, "Invalid escape sequence \\" .. c end
            buf = buf .. repl
        else
            if c == quotetype then                                  -- end of quoted string
                table.insert(rv, buf)
            elseif quotetype == nil and (c == '"' or c == '"') then -- start of quoted string
                quotetype = c
            elseif c == " " then                                    -- arg split
                table.insert(rv, buf)
                buf = ""
            else -- normal letter
                buf = buf .. c
            end
        end
    end

    if quotetype then return false, "Unterminated string" end
    return true, rv
end

function ap.extractQuotedSegments(...)
    local ok, rv = ap.tryExtractQuotedSegments(...)
    if ok then return rv else error(rv) end
end

function ap.parser(progName, intro)
    if type(progName) == "table" then
        error("[argparse] argparse.parser must be called as a function")
    end
    if type(progName) ~= "string" then
        error("[argparse] program name must be string")
    end
    local parser = {
        singleCharArgs = {},
        fullArgs = {},
        argsWithExtension = {},
        argsWithArg = {},
        positionalArgs = {},
        descrptions = {},
        progName = progName,
        intro = intro or "",
    }

    function parser:optional(arg, desc)
        desc = desc or "missing description"
        if #arg == 0 then
            error("[argparse] arguments must have at least one character!")
        elseif #arg == 1 then
            self.singleCharArgs[arg] = desc
        else
            self.fullArgs[arg] = desc
        end
        return self
    end

    function parser:withExtension(arg, desc)
        desc = desc or "missing description"
        self.argsWithExtension[arg] = desc
        return self
    end

    function parser:withArg(arg, desc)
        desc = desc or "missing description"
        self.argsWithArg[arg] = desc
        return self
    end

    function parser:positional(arg, desc)
        desc = desc or "missing description"
        table.insert(self.positionalArgs, arg)
        table.insert(self.descrptions, desc)
        return self
    end

    function parser:help()
        local h = "Help for '"
            .. self.progName
            .. "'\nsyntax: "
            .. self.progName
            .. " [ -h | -?"
        for arg, desc in pairs(self.singleCharArgs) do
            h = h .. " | -" .. arg
        end
        for arg, desc in pairs(self.fullArgs) do
            h = h .. " | --" .. arg
        end
        for arg, desc in pairs(self.argsWithExtension) do
            h = h .. " | --" .. arg .. "=<arg>"
        end
        for arg, desc in pairs(self.argsWithArg) do
            h = h .. " | --" .. arg .. " <arg>"
        end
        h = h .. " ]"
        for _, argName in ipairs(self.positionalArgs) do
            h = h .. " <" .. argName .. ">"
        end
        if #self.intro > 0 then
            h = h .. "\n" .. self.intro
        end
        h = h .. "\n  -h/-? \tprint help"
        for arg, desc in pairs(self.singleCharArgs) do
            h = h .. "\n  -" .. arg .. " \t" .. desc
        end
        for arg, desc in pairs(self.fullArgs) do
            h = h .. "\n  --" .. arg .. " \t" .. desc
        end
        for arg, desc in pairs(self.argsWithExtension) do
            h = h .. "\n  --" .. arg .. "=<arg> \t" .. desc
        end
        for arg, desc in pairs(self.argsWithArg) do
            h = h .. "\n  --" .. arg .. " <arg> \t" .. desc
        end
        for i, argName in ipairs(self.positionalArgs) do
            h = h .. "\n  <" .. argName .. "> \t" .. self.descrptions[i]
        end
        return h
    end

    function parser:parse(...)
        -- TODO handle quotes
        -- local ok, realArgs = ap.tryExtractQuotedSegments(...)
        local ok, realArgs = true, table.pack(...)
        if not ok then
            return false, "Argument error: " .. realArgs .. "\n" .. self:help()
        end
        local onlyPositional = false
        local optionals = {}
        local positionals = {}
        local argWithArg = nil
        local nextFreePos = 1
        for i, arg in ipairs(realArgs) do
            if argWithArg then
                optionals[argWithArg] = arg
                argWithArg = nil
                goto continue
            end
            if not onlyPositional and #arg >= 2 then
                if arg == "--" then
                    onlyPositional = true
                    goto continue
                elseif string.startsWith(arg, "--") then
                    local stripped = string.sub(arg, 3)
                    for _, knownFull in ipairs(self.fullArgs) do
                        if stripped == knownFull then
                            optionals[stripped] = true
                            goto continue
                        end
                    end
                    for _, knownArgWithArg in ipairs(self.argsWithArg) do
                        if stripped == knownArgWithArg then
                            argWithArg = stripped
                            goto continue
                        end
                    end
                    for _, knownWithExt in ipairs(self.argsWithExtension) do
                        if string.startsWith(stripped, knownWithExt .. "=") then
                            optionals[stripped] = string.sub(stripped, #knownWithExt + 2)
                            goto continue
                        end
                    end
                    return false,
                        string.format("Argument error: unknown argument '%s' at #%d!", arg, i) .. "\n" .. self:help()
                elseif string.startsWith(arg, "-") then
                    for j = 2, #arg do
                        local c = string.sub(arg, j, j)
                        if c == "h" or c == "?" then
                            return false, self:help()
                        end
                        if not self.singleCharArgs[c] then
                            return false,
                                string.format("Argument error: unknown argument '-%s' at #%d!", c, i) ..
                                "\n" .. self:help()
                        end
                        optionals[c] = true
                    end
                    goto continue
                end
            end
            if #positionals >= #self.positionalArgs then
                return false, "Argument error: too many positional arguments!\n" .. self:help()
            end
            positionals[self.positionalArgs[nextFreePos]] = arg
            nextFreePos = nextFreePos + 1

            ::continue::
        end

        if argWithArg then
            return false, string.format("Argument error: missing argument at #%d!", #arg + 1) .. "\n" .. self:help()
        end

        return true, optionals, positionals
    end

    return parser
end

return ap
