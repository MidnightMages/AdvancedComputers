local stringBuffer = ""
clear()
print("LUA Shell:")
printInline(">> ")
while true do
    computer.waitForMachineEvent() -- infinite waittime
    local eventName, key = computer.getMachineEvent()
    if eventName == "keyTyped" then
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
                break
            end
            local res, err = load(stringBuffer, "instr", "t", _G)
            stringBuffer = ""
            if not res then
                print("Error: ", err)
            else
                local rvs = table.pack(xpcall(res,function() print(debug.traceback("Execution error:\n")) end))
                if rvs[1] and #rvs>1 then
                    print(table.unpack(rvs, 2))
                end
            end
            printInline(">> ")
        elseif key ~= "\b" then
            stringBuffer = stringBuffer .. key
        end
        -- print(stringBuffer)
    end
end