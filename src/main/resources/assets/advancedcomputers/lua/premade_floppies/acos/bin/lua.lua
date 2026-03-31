local kernel = require("kernel")

local stringBuffer = ""
local function charTyped(key) -- return whether to exit
    if key == "\b" then
        if #stringBuffer > 0 then
            printInline(key)
            stringBuffer = stringBuffer:sub(1, #stringBuffer - 1)
        end
    else
        printInline(key)
    end

    if key == "\n" then
        if stringBuffer == "exit()" then
            return true
        end
        local res, err = load(stringBuffer, "", "t", _G)
        stringBuffer = ""
        if not res then
            print("Error: ", err)
        else
            local rvs = table.pack(pcall(res))
            --[[
            local rvs = table.pack(xpcall(res,function(msg)
                local trcb = debug.traceback("X-ERR: " .. tostring(msg), 2)
                for i = 1, 4, 1 do
                    trcb = trcb:sub(1, trcb:match("^.*()\n") - 1)
                end
                print(trcb)
            end))]]
            rvs[1] = rvs[1] and "OK" or "ERROR"
            --if #rvs > 1 then
            print(table.unpack(rvs))
            --end
        end
        printInline(">> ")
    elseif key ~= "\b" then
        stringBuffer = stringBuffer .. key
    end
end

printInline(">> ")
local keepRunning = true
kernel:registerEventCallback("charTyped", function(...)
    if charTyped(select(2,...)) then keepRunning = false end
end)
while keepRunning do sleep(1) end