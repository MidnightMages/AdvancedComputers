component = components
local ok, rv = xpcall(function()

	local gpu = component:getFirst("gpu")
	local textBuffer = nil
	local screenSizeX, screenSizeY = 128, 25
	local cursorX, cursorY = 0, 0
	if gpu ~= nil then
		textBuffer = gpu:newBuffer(screenSizeX, screenSizeY)
		gpu:assignBuffer(textBuffer, 0)
	end

	local function setUpPrinting(funcName)
		local oldFunc = _ENV[funcName]
		_ENV[funcName] = function(...)
			if oldFunc ~= nil then -- try print via the original global
				oldFunc(...)
			end

			local someScreen = component:getFirst("screen")
			if someScreen ~= nil then -- try old printing api
				someScreen[funcName](someScreen,...)
			end

			if textBuffer ~= nil then
				local function doNewline()
					textBuffer:rotRows(1)
					textBuffer:clearRow(0)
				end

				local textToPrint = table.concat(table.pack(...), " ")
				for i = 1, #textToPrint do
					local currChar = textToPrint:sub(i,i)
					if currChar == "\b" then
						cursorX = math.max(cursorX-1, 0)
						textBuffer:set(cursorX, cursorY, ' ', nil, nil)
					else
						textBuffer:set(cursorX, cursorY, currChar, nil, nil)
						cursorX = cursorX + 1
						if currChar == "\n" then cursorX = screenSizeX end
						if cursorX == screenSizeX then
							cursorX = 0
							cursorY = cursorY+1
							if cursorY >= screenSizeY then
								cursorY = screenSizeY-1
								doNewline()
							end
						end
					end
				end
				if funcName == "print" then
					cursorY = cursorY + 1
					cursorX = 0
					if cursorY >= screenSizeY then
						cursorY = screenSizeY-1
						doNewline()
					end
				else -- printInline
					assert(funcName == "printInline")
				end
			end
		end
	end

	setUpPrinting("print")
	setUpPrinting("printInline")
	_G.components = {}
	local computer = component:getFirst("computer") -- TODO rename component to components
	local idx = 1
	for t, a in component:list() do
	   print(t, a.componentType)
	   computer.nvram.test = 123
	   print(computer.nvram.test)
	   computer.nvram.test = "bla"
	   --component:getFirst("bios"):setData("testbiosdata")
	   --print("bios data:",component:getFirst("bios"):getData())
	   print(computer.nvram.test)
	   if t == "massStorage" then
		  --print("has boot file? ", a.fileExists("boot.lua"))
		  if a:fileExists("boot.lua") then
			 print("Bootable file found on storage #"..idx.." - reading...")
			 local code = a:open("boot.lua"):read()
			 print("Compiling boot.lua...")
			 --print(code, type(code))
			 _G.bootDrive = a
			 local f = load(code, "boot.lua")
			 if not f then error("bios boot compilation failed") end
			 print("Booting...")
	---@diagnostic disable-next-line: need-check-nil         
			 local ok, err = xpcall(f, debug.traceback)
			 if not ok then
				local etext = "bios boot error: "..tostring(err)
				print(etext)
				error(etext)
			 end
			 return
		  else
			 idx = idx+1
		  end
	   end
	end 
	print("No bootable medium found!")
end, debug.traceback)
if not ok then
	error("bios error: "..tostring(rv), 0)
end
print("system has exited")
--error("No bootable filesystem found!")