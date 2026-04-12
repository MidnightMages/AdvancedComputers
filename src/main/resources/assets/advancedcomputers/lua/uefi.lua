local ok, rv = xpcall(function()

	local gpu = components:getFirst("gpu")
	local textBuffer = nil
	local screenSizeX, screenSizeY = 110, 44
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
				cursorX, cursorY = textBuffer:pasteText(cursorX, cursorY, "SCROLL_SPILL_CLEAR", textToPrint)
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
	if computer.nvram ~= nil then -- nvram is just a magical table that can only store primitives, but is readwrite and persistent across restarts
		print("nvram is available")
	else
		print("nvram is unavailable")
	end

	local idx = 1
	for t, a in components:list() do
	   print(t, a.componentType)
	   if t == "massStorage" then
		  --print("has boot file? ", a.fileExists("boot.lua"))
		  if a:fileExists("boot.lua") then
			 print("Bootable file found on storage #"..idx.." - reading...")
			 local handle = a:open("boot.lua")
			 local code = handle:read(-1)
			 handle:close()
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