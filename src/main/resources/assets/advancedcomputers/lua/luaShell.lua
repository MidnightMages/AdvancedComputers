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
                -- TODO table.pack
                local ok, rv = xpcall(res,function() print(debug.traceback("Execution error:\n")) end)
                -- TODO only print if non-empty
                if ok then print(rv) end
            end
            printInline(">> ")
        elseif key ~= "\b" then
            stringBuffer = stringBuffer .. key
        end
        -- print(stringBuffer)
    end
end