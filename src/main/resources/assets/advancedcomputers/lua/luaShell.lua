local stringBuffer = ""
while true do
    computer.waitForMachineEvent() -- infinite waittime
    local events = computer.getMachineEvents()
    for i = 1, #event do
        local eventName = event[i][1]
        if eventName == "keyPressed" then
            local key = events[i][2]
            if key == "\n" then
                local res, err = load(stringBuffer)
                stringBuffer = ""
                if not res then
                    print("Error: ", err)
                else
                    xpcall(res,function() print(debug.traceback("Error during execution:\n")) end)
                end
            end
            stringBuffer = stringBuffer .. key
            print(stringBuffer)
        end
    end
end