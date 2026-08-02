--[[
Screen Buffer manipulation library
]]

local screen = {}

function screen.bindPrint(textBuffer)
    local cursorX, cursorY = 0, 0 -- to be captured by new prints
    local function makePrint(append)
        return function(...)      -- new printing to screen
            local packed = table.pack(...)
            for i = 1, #packed do
                if packed[i] == nil then packed[i] = "nil" end
            end
            cursorX, cursorY = textBuffer:pasteText(
                cursorX, cursorY, "SCROLL_SPILL_CLEAR",
                table.concat(packed, "\t") .. append)
        end
    end
    return makePrint("\n"), makePrint("")
end

return screen
