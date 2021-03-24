---@class SomeClass

---@alias DirectAlias SomeClass

---@alias IndirectAlias DirectAlias

---@alias ReallyIndirectAlias IndirectAlias

---@type SomeClass
local someClass

---@type ReallyIndirectAlias
local reallyIndirectAlias = someClass

reallyIndirectAlias = <error descr="Type mismatch. Required: 'ReallyIndirectAlias' Found: '\"invalid\"'">'invalid'</error>
reallyIndirectAlias = someClass
someClass = reallyIndirectAlias


---@class SomeGenericClass<T: string>

---@alias DirectGenericAlias<T: string> SomeGenericClass<T>

---@alias IndirectGenericAlias<T: string> DirectGenericAlias<T>

---@alias ReallyIndirectGenericAlias<T: string> IndirectGenericAlias<T>

---@type SomeGenericClass<string>
local someGenericClass

---@type ReallyIndirectGenericAlias<string>
local reallyIndirectGenericAlias

reallyIndirectGenericAlias = <error descr="Type mismatch. Required: 'ReallyIndirectGenericAlias<string>' Found: '\"invalid\"'">'invalid'</error>
reallyIndirectGenericAlias = someGenericClass
someGenericClass = reallyIndirectGenericAlias



---@alias InfiniteAlias1 string|InfiniteAlias2
---@alias InfiniteAlias2 number|InfiniteAlias1

---@type InfiniteAlias1
local infiniteAlias1

---@type InfiniteAlias2
local infiniteAlias2

infiniteAlias1 = <error descr="Type mismatch. Required: 'InfiniteAlias1' Found: 'table'">{}</error>
infiniteAlias1 = 1
infiniteAlias1 = 'string'
infiniteAlias1 = infiniteAlias2
infiniteAlias2 = <error descr="Type mismatch. Required: 'InfiniteAlias2' Found: 'table'">{}</error>
infiniteAlias2 = 1
infiniteAlias2 = 'string'
infiniteAlias2 = infiniteAlias1


---@alias InfiniteGenericAlias1<T> T|InfiniteGenericAlias2<T>

---@alias InfiniteGenericAlias2<T> InfiniteGenericAlias1<T>

---@type InfiniteGenericAlias1<string>
local infiniteGenericAlias1

---@type InfiniteGenericAlias2<string>
local infiniteGenericAlias2

infiniteGenericAlias1 = <error descr="Type mismatch. Required: 'InfiniteGenericAlias1<string>' Found: '1'">1</error>
infiniteGenericAlias1 = 'string'
infiniteGenericAlias1 = infiniteGenericAlias2

infiniteGenericAlias2 = <error descr="Type mismatch. Required: 'InfiniteGenericAlias2<string>' Found: '1'">1</error>
infiniteGenericAlias2 = 'string'
infiniteGenericAlias2 = infiniteGenericAlias1


---@alias JSONObject table<string, JSONValue>
---@alias JSONArray JSONValue[]
---@alias JSONContainer JSONObject | JSONArray
---@alias JSONValue JSONContainer | number | string | boolean | nil

---@type JSONValue
local value

value = 1
value = true
value = "a string"
value = {}
value = {1, 2, 3}
value = {
    a = 1,
    b = 2,
    c = 3
}
value = nil
value = {
    a = {
        b = 1,
        c = {
            1,
            2,
            {
                d = false,
                e = nil
            }
        }
    }
}
value = <error descr="Type mismatch. Required: 'JSONValue' Found: 'fun(): void'">function() end</error>
value = <error descr="Type mismatch. Required: 'JSONValue' Found: 'thread'">coroutine.create(function() end)</error>
value = {
    <error descr="Type mismatch. Required array index: '1' Found non-contiguous index: '3'">[3] = "bad"</error>
}
