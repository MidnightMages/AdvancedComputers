local ok, rv = xpcall(function()

	local gpu = components:getFirst("gpu")
	local textBuffer = nil
	local screenSizeX, screenSizeY = 128, 25
	local cursorX, cursorY = 0, 0
	if gpu ~= nil then
		textBuffer = gpu:newBuffer(screenSizeX, screenSizeY)
		for t,v in components:list() do
			if t == "screen" then
				gpu:assignBuffer(textBuffer, v)
			end
		end
	end

	local function setUpPrinting(funcName)
		local oldFunc = _ENV[funcName]
		_ENV[funcName] = function(...)
			if oldFunc ~= nil then -- try print via the original global
				oldFunc(...)
			end

			local someScreen = components:getFirst("screen")
			if someScreen ~= nil and someScreen[funcName] then -- try old printing api
				someScreen[funcName](someScreen,...)
			end

			if textBuffer ~= nil then
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
								textBuffer:newline()
							end
						end
					end
				end
				if funcName == "print" then
					cursorY = cursorY + 1
					cursorX = 0
					if cursorY >= screenSizeY then
						cursorY = screenSizeY-1
						textBuffer:newline()
					end
				else -- printInline
					assert(funcName == "printInline")
				end
			end
		end
	end

	setUpPrinting("print")
	setUpPrinting("printInline")
	local computer = components:getFirst("computer")
	local idx = 1
	for t, a in components:list() do
	   print(t, a.componentType)
	   computer.nvram.test = 123
	   print(computer.nvram.test)
	   computer.nvram.test = "bla"
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