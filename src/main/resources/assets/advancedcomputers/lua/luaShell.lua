local stringBuffer = ""
while true do
    computer.waitForMachineEvent() -- infinite waittime
    local event = computer.getMachineEvent()
    local eventName = event[1]
    if eventName == "keyPressed" then
        local key = event[2]
        if key == "\n" then
            if stringBuffer == "exit" then
                break
            end
            local res, err = load(stringBuffer)
            stringBuffer = ""
            if not res then
                print("Error: ", err)
            else
                xpcall(res,function() print(debug.traceback("Execution error:\n")) end)
            end
        end
        stringBuffer = stringBuffer .. key
        print(stringBuffer)
    end
end