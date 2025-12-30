component = components
local ok, rv = xpcall(function()
	local oldPrint = print
	print = function(...)
		if oldPrint then oldPrint(...) end
		component:getFirst("screen"):print(...)
	end
	local oldPrintInline = printInline
	printInline = function(...)
		if oldPrintInline then oldPrintInline(...) end
		component:getFirst("screen"):printInline(...)
	end
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