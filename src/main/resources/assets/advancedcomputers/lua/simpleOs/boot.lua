local allCoroutines = {} -- ({coroutine co} CONCAT object[] args)[] == (object[])[]

local function coroutineDispatcher()
    local i = 1
    while #allCoroutines > 0 do
        if i > #allCoroutines then i = 1 end

        local currTuple = allCoroutines[i]
        local currCo = currTuple[1]
        local cResultPacked = table.pack(coroutine.resume(table.unpack(currTuple)))
        if not cResultPacked[1] then -- if coroutine errored (coroutine.status being dead should a)
            error("Uncaught coroutine error: ", cResultPacked[2])
        else
            -- if the coroutine is running then this would mean that this dispatcher executes itself, which it cannot, so this case doesnt need handling
            -- normal would mean the coroutine ran another coroutine, which shouldnt need handling, as it wouldnt yield control to this dispatcher in that case
            -- suspended is handled in the following,
            -- and the only other case should be dead            
            if coroutine.status(currCo) == "suspended" then -- store the coroutine again in the queue at the same position
                cResultPacked[1] = currCo
                allCoroutines[i] = cResultPacked
                i = i + 1 -- increment i to point to the next coroutine
            else -- this should only be hit by the dead case, but we will chuck in an assertion just in case
                assert(coroutine.status(currCo) == "dead", "Dispatcher assertion fail: Coroutine status is actually "..tostring(coroutine.status(currCo)))
                -- if it is dead, we dont want to execute it anymore, i.e remove it from the queue
                table.remove(allCoroutines, i) -- dont modify i as it will already point to the next element
            end
        end
    end
    error("All coroutines have exited. Shutdown time?")
end

-- TODO DOCUMENT NEW TABLE
_G["osCoroutine"] = {}

-- TODO DOCUMENT NEW FUNCTION 
_G.osCoroutine["enqueue"] = function(co, ...) 
    table.insert(allCoroutines, {co, table.pack(...)})
end

-- TODO DOCUMENT NEW FUNCTION 
_G.osCoroutine["createAndEnqueue"] = function(f, ...)
    osCoroutine.enqueue(coroutine.create(f), ...)
end

local function boot()
    
end

osCoroutine.createAndEnqueue(boot)
coroutineDispatcher()