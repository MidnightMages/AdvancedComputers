---@type string
local biosData = components:getFirst("computer").uefi.data
-- biosData = [[
-- print("")

-- ]]

local inputOk = type(load(biosData)) == "function"
print("initial bios size: ", #biosData, "does compile? ", inputOk)
if not inputOk then return end

local nextChar = 1
local function getCharAt(idx)
    return biosData:sub(idx,idx)
end
local function getNextChar()
    local rv = getCharAt(nextChar)
    nextChar = nextChar + 1
    return rv
end


local a = "\""
-- tokenize strings / operators / other code tokens
local stringStartChars = {["'"] = true, ['"'] = true} -- [[, [=[, [==[, ...
local isWhitespace = {
    [" "] = true, ["\t"] = true, ["\r"] = true, ["\n"] = true
}

local isItsOwnToken = {
    ["="] = true, ["."] = true, ["+"] = true, ["*"] = true
}



local function tokenize()
    local result = {}

    local stringType = nil
    local consumedString = ""
    local function storeCurrentToken()
        if #consumedString > 0 then
            table.insert(result, consumedString)
            consumedString = ""
        end
    end

    while nextChar <= #biosData do
        if stringType == nil then -- not inside a string
            local currChar = getNextChar()

            if (currChar == "-") and (getCharAt(nextChar) == "-") then -- start of comment --> consume the entire comment
                getNextChar() -- = second dash
                local capture = (getCharAt(nextChar) == '[') and string.match(biosData, "^(=*)%[", nextChar+1)
                if capture then -- multiline comment
                    local _, endPos = string.find(biosData, "]"..capture.."]", nextChar+1, true)
                    nextChar = assert(endPos, "somehow endpos was not found?")+1
                else -- singleline comment
                    while getNextChar() ~= "\n" do end
                end
            elseif isWhitespace[currChar] then -- we encountered a whitespace char and are not in a string --> token finished
                storeCurrentToken()
            elseif isItsOwnToken[currChar] then
                storeCurrentToken()
                consumedString = currChar
                storeCurrentToken()
            elseif stringStartChars[currChar] then -- we are starting a simple string
                storeCurrentToken()
                stringType = currChar
                consumedString = currChar
                --print("starting simple string "..currChar.." at "..tostring(nextChar).."-->"..tostring(biosData:sub(nextChar-1,nextChar+2)))
                --print("next char after starting will be: ",tostring(getCharAt(nextChar)))
            else
                local capture = (currChar == "[") and string.match(biosData, "^(=*)%[", nextChar)
                if capture then -- we are starting a complex string
                    --print("starting string at [["..tostring(capture).."]]<<")
                    storeCurrentToken()
                    stringType = "]"..capture.."]"
                    nextChar = nextChar + #capture + 1
                    consumedString = "["..capture.."["
                else -- regular text
                    consumedString = consumedString .. currChar
                end
            end
        else -- inside string
            local endIndex = string.find(biosData, stringType, nextChar, true)
            --print("ending string at"..tostring(endIndex))
            assert(endIndex ~= nil, "unterminated string of type >>"..stringType.."<<. nextChar: "..tostring(nextChar))
            consumedString = consumedString .. biosData:sub(nextChar, endIndex+#stringType-1)
            storeCurrentToken() -- string is done now
            nextChar = endIndex + #stringType
            --print("next char will be: ", getCharAt(nextChar))
            stringType = nil
        end
    end
    storeCurrentToken()
    assert(stringType == nil, "unterminated string of type >>"..tostring(stringType).."<<")
    return result
end

-- token postprocessing
local tokens = tokenize()
local ts = ""
for i=1,#tokens do
    if tokens[i] == "local" then
        if tokens[i+1] and string.endsWith(tokens[i+1],"<const>") then -- replace <const> with nothing as we already checked that using a load() call
            tokens[i+1] = tokens[i+1]:sub(1,-#("<const>")-1)
        end
    end
end

-- emit
for _, token in ipairs(tokens) do
    --print(token)
    if #ts > 0 then
        local tokenEnd = ts:sub(-1)
        local nextStart = token:sub(1,1)
        local identChars = "[%w_]"
        local needPadding = (tokenEnd:match(identChars) and nextStart:match(identChars))
            or (tokenEnd == ">" and nextStart == "=")
            ;


        if needPadding then
            ts = ts .. " "
        end
    end
    ts = ts .. token
end
print("-------------------------------")
print(ts)
require("filesystem"):writeAllText("/newUefi.lua",ts)
local compiles, eMsg = load(ts)
print("without comments: ", #ts, "does compile? ", type(compiles) == "function", eMsg)
while true do
    sleep(100)    
end