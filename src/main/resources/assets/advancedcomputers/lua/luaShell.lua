computer.subMachineEvent("keyTyped")
computer.subMachineEvent("textPasted")
local stringBuffer = ""
clear()
print("LUA Shell:")
printInline(">> ")

local function keyTyped(key) -- return whether to exit
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
        local res, err = load(stringBuffer, "cmd", "t", _G)
        stringBuffer = ""
        if not res then
            print("Error: ", err)
        else
            local rvs = table.pack(xpcall(res,function(msg)
                local trcb = debug.traceback("X-ERR: " .. tostring(msg), 2)
                for i = 1, 4, 1 do
                    trcb = trcb:sub(1, trcb:match("^.*()\n") - 1)
                end
                print(trcb)
            end))
        end
        printInline(">> ")
    elseif key ~= "\b" then
        stringBuffer = stringBuffer .. key
    end
end

while true do
    computer.waitForMachineEvent() -- infinite waittime
    local eventName, arg1 = computer.getMachineEvent()
    if eventName == "keyTyped" then
        if keyTyped(arg1) then break end
    elseif eventName == "textPasted" then
        for i = 1, #arg1 do
            if keyTyped(arg1:sub(i,i)) then break end
        end
    end
end