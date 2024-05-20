local stringBuffer = ""
clear()
print("LUA Shell:")
printInline(">> ")
while true do
    computer.waitForMachineEvent() -- infinite waittime
    local eventName, key = computer.getMachineEvent()
    if eventName == "keyPressed" then
        printInline(key)
        if key == "\n" then
            if stringBuffer == "EXIT" then
                break
            end
            local res, err = load(stringBuffer)
            stringBuffer = ""
            if not res then
                print("Error: ", err)
            else
                xpcall(res,function() print(debug.traceback("Execution error:\n")) end)
            end
            printInline(">> ")
        else
            stringBuffer = stringBuffer .. key
        end
        -- print(stringBuffer)
    end
end