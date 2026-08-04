--[[
A little implementation of a text editor inspired by the gnu-nano editor
]]

local argParser = require("argparse").parser("nano", "A little text editor inspired by GNU NANO")
    :positional("filename", "file to copy")

local ok, opt, pos = argParser:parse(...)
if not ok then
    print(opt)
    return -1
end

local DEFAULT_NAME <const> = "unnamed.txt"
local filename = pos.filename

local fs = require "filesystem"
local kernel = require "kernel"
local insert = table.insert


-- try loading the file
local data = {}
if filename ~= nil then
    if fs:fileExists(filename) then
        data = string.split(fs:readAllText(filename), "\n")
        -- normalize line endings from possibly \r\n to \n
        for i = 1, #data do
            local l = data[i]
            if string.endsWith(l, "\r") then
                data[i] = string.sub(l, 1, #l - 1)
            end
        end
    end
end
if not filename then
    if not fs:fileExists(DEFAULT_NAME) then
        filename = DEFAULT_NAME
    else
        local i = 1
        while fs:fileExists(string.format("unnamed_%d.txt", i)) do
            i = i + 1
        end
        filename = string.format("unnamed_%d.txt", i)
    end
end
filename = filename or DEFAULT_NAME
if #data == 0 then
    insert(data, "")
end


-- UI setup
local buffer = kernel:newTextBuffer()


-- helpers
local function padRight(str, len, filler)
    if #str >= len then
        return str
    end
    filler = filler or " "
    local res = str .. string.rep(filler, math.max(0, (len - #str) // #filler))
    return #res >= len and res or res .. string.sub(filler, 1, len - #res)
end
local function padLeft(str, len, filler)
    if #str >= len then
        return str
    end
    filler = filler or " "
    local res = string.rep(filler, math.max(0, (len - #str) // #filler)) .. str
    return #res >= len and res or string.sub(filler, 1, len - #res) .. res
end



-- set up screen
local WIDTH <const> = buffer.width
local HEIGHT <const> = buffer.height
local VISIBLE_LINES <const> = HEIGHT - 3
buffer:pasteText(0, 0, "FILL_CLIP_CLEAR", string.rep("\n", HEIGHT))
-- state
local running = true
local scrollPos = 1
local showLineNums = true
local mode = "edit"
local hasUnsavedChanges = false
-- caret
local cy = 1
local cx = 1
local hiddenByCaret = nil
local function toScreenPos(x, y)
    return math.min(x + (showLineNums and #tostring(#data) + 1 or 0), WIDTH) - 1, y - scrollPos + 1
end
local function clearCaret()
    if hiddenByCaret then
        -- restore character
        local x, y = toScreenPos(cx, cy)
        buffer:set(x, y, hiddenByCaret, nil, nil)
        hiddenByCaret = nil
    end
end



-- editing UI
-- top line
local shortFileName = filename
    or #filename <= 32 and filename
    or string.sub(filename, 1, 32)
local function writeTopLine()
    buffer:pasteText(0, 0, "STOP", padRight(
        string.format("███ AdvancedOS NANO ███ %s ███", shortFileName), WIDTH, "█"))
end
writeTopLine()
-- bottom line
local function writeBottomLine()
    buffer:pasteText(0, HEIGHT - 1, padRight("^O Write Out    ^X Exit         ^L Line Nums    ", WIDTH))
end
writeBottomLine()
local function updateLineCnt()
    buffer:pasteText(0, HEIGHT - 2, "STOP_CLEAR", padRight(
        string.format("███ [ editing a file with %d line%s ] ", #data, #data > 1 and "s" or ""),
        WIDTH, "█"))
end
updateLineCnt()
-- line helpers
local function drawLines()
    -- move screen to caret
    -- TODO handle offscreen in x
    local cdif = cy - scrollPos
    if cdif >= VISIBLE_LINES then
        scrollPos = scrollPos + (cdif - VISIBLE_LINES) + 1
    elseif cdif < 0 then
        scrollPos = scrollPos + cdif
    end
    -- print the lines
    local lines = table.move(data, scrollPos, scrollPos - 1 + VISIBLE_LINES, 1, {})
    if showLineNums then
        local maxlen = #tostring(#data)
        for i = 1, #lines do
            local l = padLeft(tostring(scrollPos - 1 + i), maxlen)
            l = l .. " "
            l = l .. lines[i]
            l = #l <= WIDTH and l or string.sub(i, #l - 1) .. ">"
            lines[i] = l
        end
    else
        for i = 1, #lines do
            local l = lines[i]
            l = #l <= WIDTH and l or string.sub(i, #l - 1) .. ">"
        end
    end
    for i = #lines + 1, VISIBLE_LINES do
        lines[i] = ""
    end
    lines[#lines] = padRight(lines[#lines], WIDTH, " ") -- overwrite the rest of the last line
    buffer:pasteText(0, 1, "FILL_CLIP_CLEAR", table.concat(lines, "\n"))
end
drawLines()



-- write mode UI
local function wTopLine()
    buffer:pasteText(0, HEIGHT - 2, "STOP_CLEAR",
        padRight("███ [ choosing a filename to save the file ] ", WIDTH, "█"))
end

local function wErrorLine(err)
    buffer:pasteText(0, HEIGHT - 2, "STOP_CLEAR",
        padRight(string.format("███ [ error '%s', choosing filename ] ", err), WIDTH, "█"))
end

local function wBottomLine()
    buffer:pasteText(0, HEIGHT - 1, "STOP_CLEAR",
        padRight("^C: return to editing, ENTER: save > " .. filename .. "_", WIDTH))
end



-- exiting mode UI
local function exitingBottomLine()
    buffer:pasteText(0, HEIGHT - 1, "STOP_CLEAR",
        padRight("UNSAVED CHANGES!!!   ^C: return to editing, ENTER: exit anyways", WIDTH))
end



-- edit mode handlers
local function charTyped(char)
    clearCaret()
    local l = data[cy]
    if char == "\n" then
        -- new line
        if cx <= 1 then
            -- move entire line down
            table.insert(data, cy, "")
        elseif cx > #l then
            -- clean newline
            table.insert(data, cy + 1, "")
        else
            -- split the old line
            local old = string.sub(l, 1, cx - 1)
            local new = string.sub(l, cx, #l)
            data[cy] = old
            table.insert(data, cy + 1, new)
        end
        cx = 1
        cy = cy + 1
        updateLineCnt()
    elseif char == "\b" then
        -- remove char
        if cx <= 1 then
            -- start of the line, wrap
            if cy > 1 then
                -- there is a line above
                table.remove(data, cy)
                cy = cy - 1
                cx = #l + 1
                l = data[cy] .. l
                updateLineCnt()
            else
                -- do nothing
                return
            end
        elseif cx == 2 then
            -- first char
            l = string.sub(l, 2, #l)
            cx = 1
        elseif cx > #l then
            -- end of the line
            l = string.sub(l, 1, #l - 1)
            cx = cx - 1
        else
            -- middle of the line
            l = string.sub(l, 1, cx - 2) .. string.sub(l, cx, #l)
            cx = cx - 1
        end
        data[cy] = l
    else
        if cx <= 1 then
            -- start of the line
            l = char .. l
        elseif cx > #l then
            -- end of the line
            l = l .. char
        else
            -- middle of the line
            l = string.sub(l, 1, cx - 1) .. char .. string.sub(l, cx, #l)
        end
        data[cy] = l
        cx = cx + 1
    end
    drawLines()
    hasUnsavedChanges = true
end

local function keyPressed(stRep, keyCode, scanCode, mods)
    clearCaret()
    if keyCode == 266 then     -- PAGE_UP
        cy = math.max(1, cy - VISIBLE_LINES)
        cx = math.min(#data[cy] + 1, cx)
    elseif keyCode == 267 then -- PAGE_DOWN
        cy = math.min(#data, cy + VISIBLE_LINES)
        cx = math.min(#data[cy] + 1, cx)
    elseif keyCode == 268 then -- HOME
        cx = 1
    elseif keyCode == 269 then -- END
        cx = #data[cy] + 1
    elseif keyCode == 262 then -- RIGHT
        if cx > #data[cy] then
            -- wrap line
            if cy < #data then
                cy = cy + 1
                cx = 1
            end
        else
            cx = cx + 1
        end
    elseif keyCode == 263 then -- LEFT
        if cx <= 1 then
            -- wrap line
            if cy > 1 then
                cy = cy - 1
                cx = #data[cy] + 1
            else
                -- do nothing
                return
            end
        else
            cx = cx - 1
        end
    elseif keyCode == 264 then                -- DOWN
        cy = math.min(#data, cy + 1)
        cx = math.min(#data[cy] + 1, cx)
    elseif keyCode == 265 then                -- UP
        cy = math.max(1, cy - 1)
        cx = math.min(#data[cy] + 1, cx)
    elseif keyCode == 0x4C and mods == 2 then -- ^L
        -- toggle line numbers
        showLineNums = not showLineNums
        drawLines()
    elseif keyCode == 0x4F and mods == 2 then -- ^O
        -- save
        -- enter write mode
        mode = "write"
        wTopLine()
        wBottomLine()
        return
    elseif keyCode == 0x58 and mods == 2 then -- ^X
        -- quit
        mode = "exiting"
        if hasUnsavedChanges then
            exitingBottomLine()
        else
            -- clean exit
            running = false
        end
        return
    end
    local cdif = cy - scrollPos
    if cdif >= VISIBLE_LINES or cdif < 0 then
        -- caret is off screen in y
        -- TODO handle offscreen in x
        drawLines()
    end
end

local function textPasted(text)
    local nuLines = string.split(text, "\n")
    local len = #nuLines
    local lastLineIdx = cy + len - 1
    if cx <= 1 then
        -- start of the line
        for i = 1, len - 1 do
            -- plain insert all lines but the last
            table.insert(data, cy + i - 1, nuLines[i])
        end
        -- prepend the last line to the existing line
        data[lastLineIdx] = nuLines[len] .. data[lastLineIdx]
        -- set caret
        cy = lastLineIdx
        cx = #nuLines[len] + 1
    elseif cx > #data[cy] then
        -- end of the line
        -- append first line of new data
        data[cy] = data[cy] .. nuLines[1]
        for i = 2, len do
            -- plain insert the rest of the lines
            table.insert(data, cy + i - 1, nuLines[i])
        end
        -- set caret
        cy = lastLineIdx
        cx = #data[lastLineIdx] + 1
    else
        -- middle of the line
        local l = data[cy]
        local left = string.sub(l, 1, cx - 1)
        local right = string.sub(l, cx, #l)
        -- ignore right for now and append to the the left part of the line
        data[cy] = left .. nuLines[1]
        for i = 2, len do
            -- plain insert the rest of the lines
            table.insert(data, cy + i - 1, nuLines[i])
        end
        -- fixup the last line by appending right
        local nuCX = #data[lastLineIdx] + 1
        data[lastLineIdx] = data[lastLineIdx] .. right
        -- set caret
        cy = lastLineIdx
        cx = nuCX
    end
    drawLines()
end



-- write mode (choosing filename) handlers
local function typedInWriteMode(char)
    if char == "\n" then
        -- try writing file
        local ok, err = pcall(fs.writeAllText, fs, filename, table.concat(data, "\n"))
        if ok then
            -- return to edit mode
            mode = "edit"
            writeTopLine()
            updateLineCnt()
            writeBottomLine()
            hasUnsavedChanges = false
        else
            wErrorLine(err)
        end
    elseif char == "\b" then
        if #filename > 0 then
            filename = string.sub(filename, 1, #filename - 1)
        end
        wBottomLine()
    else
        filename = filename .. char
        wBottomLine()
    end
end

local function pressedInWriteMode(stRep, keyCode, scanCode, mods)
    if keyCode == 0x43 and mods == 2 then -- ^C
        -- return to edit mode
        mode = "edit"
        writeTopLine()
        updateLineCnt()
        writeBottomLine()
    end
end



-- exiting mode handlers
local function pressedInExiting(stRep, keyCode, scanCode, mods)
    if keyCode == 0x43 and mods == 2 then -- ^C
        -- return to edit mode
        mode = "edit"
        writeTopLine()
        updateLineCnt()
        writeBottomLine()
    elseif stRep == "\n" then -- ENTER
        -- exit anyways
        running = false
    end
end



-- register handlers
kernel:registerEventCallback("charTyped", function(...)
    if mode == "edit" then
        charTyped(select(2, ...))
    elseif mode == "write" then
        typedInWriteMode(select(2, ...))
    elseif mode == "exiting" then
        -- do nothing
    else
        error("charTyped, unknown mode " .. mode)
    end
end)

kernel:registerEventCallback("keyPressed", function(...)
    if mode == "edit" then
        keyPressed(select(2, ...))
    elseif mode == "write" then
        pressedInWriteMode(select(2, ...))
    elseif mode == "exiting" then
        pressedInExiting(select(2, ...))
    else
        error("keyPressed, unknown mode " .. mode)
    end
end)

kernel:registerEventCallback("textPasted", function(...)
    if mode == "edit" then
        textPasted(select(2, ...))
    elseif mode == "write" then
        -- do nothing
    elseif mode == "exiting" then
        -- do nothing
    else
        error("textPasted, unknown mode " .. mode)
    end
end)


-- main thread
kernel:showTextBuffer(buffer)

while running do
    sleep(0.5)
    local x, y = toScreenPos(cx, cy)
    -- blink the caret
    if hiddenByCaret then
        -- restore character
        buffer:set(x, y, hiddenByCaret, nil, nil)
        hiddenByCaret = nil
    else
        hiddenByCaret = buffer:getText(x, y)
        buffer:set(x, y, "_", nil, nil)
    end
end
