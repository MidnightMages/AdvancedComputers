--[[
Simple syscall setup for redstone interface component
]]


local assertPermission = kutils.assertPermission
local wrapComponent = kutils.wrapObject
local unwrapComponent = kutils.unwrapObject

local DRIVER_NAME <const> = "redio"
local PERMISSION <const> = "drv.redstone"
kutils.registerPermission(PERMISSION)

local syscalls = {}

local prototype = {
    componentType = "redstone",
}

function prototype:getInput(direction)
    return coroutine.yield("syscall", DRIVER_NAME, "getInput", self, direction)
end

function prototype:setOutput(direction, level)
    return coroutine.yield("syscall", DRIVER_NAME, "setOutput", self, direction, level)
end



function syscalls.getComponents()
    assertPermission(PERMISSION)
    local interfaces = {}
    for tp, elem in components:list() do
        if tp == "redstone" then
            table.insert(interfaces, wrapComponent(elem, prototype))
        end
    end
    return interfaces
end

function syscalls.getInput(wrapper, direction)
    assertPermission(PERMISSION)
    return unwrapComponent(wrapper, DRIVER_NAME):getInput(direction)
end

function syscalls.setOutput(wrapper, direction, level)
    assertPermission(PERMISSION)
    return unwrapComponent(wrapper, DRIVER_NAME):setOutput(direction, level)
end

return syscalls
