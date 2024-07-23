# Advanced Computers API

Pascal case for most things :)

### Built-in lua functions
- Computer --- main object
    - disks: Disk[] ({nil, Disk, Disk} for no disk in slot 1 and two disks after that)
    - network: Network 
    - screens: Screen[] similar to Disk
    - keyboard: Keyboard[] similar to Disk
    - other hardware components
    
Added by AdvancedOS:
- Os: os specific helpers
    - dns

- Disk:
    ```c
    {
        string driveName
        bool isManaged
        #if isManaged
            function getFiles(string path) -> string[] (filenames)
            function createFile(string path) -> bool (was created)
            function deleteFile(string path) -> void          
            function moveFile(string existingPath, string newPath) -> void  
            function openFile(string path, filemode) -> Stream [LUA FUNC]
                                   
            function getDirectories(string path) -> string[] (directories)
            function createDirectory(string path) -> bool (was created)
            function deleteDirectory(string path) -> void
            function moveDirectory(string existingPath, string newPath) -> void
            function getFreeCapacity() -> uint32
        #else
            function readBytes(uint startPos, uint size) -> byte[]
            function readBytes(uint startPos, byte[] data) -> void
            function erase() -> void
            function fill(uint startPos, uint size, byte value) -> void
        #endif            
        function getTotalCapacity() -> uint32
    }
    ```
        
- Network:
    - sendNetworkPacket(ACIP destAddress, PacketType type, byte[] data) -> bool wasSentToARouter
    - 
        
    **PacketTypes**: int
    - Network = 0
    - DNS = 1
    - Ping = 2
    - **Not selectable and also immutable types:**
        - TCP = 16
        - HTTP = 17
        - HTTPS = 18

- Screen:

# Peripherals
- Redstone in/output (dedicated block)
- Inventory (adapter)
- Furnace checkFuel & progress (adapter)
- Jukebox (adapter)

add internet card address black/whitelisting (see https://oc.cil.li/topic/2405-regarding-cve-2023-37261-oc-183-released/)
![](https://s3.hedgedoc.org/demo/uploads/5598610c-adbe-458f-851e-885537320df2.png)

intranet services:
- send chat messages
- dns
- internet
- package manager repository
    - reads files from a folder on the mc server
    - e.g. everone can upload via sftp (ssh) in to the dir that probs would have the sticky bit set so everyone can only mess with their own files
    - package manager then downloads the files from the repository and puts it on the local pc


```
ALLOW 10.0.0.17

DENY 0.* # aka 0.*.*.*
DENY 10.*
DENY 100.[64-127].*
DENY 127.*
DENY 169.254.*
DENY 172.[16-31].*
DENY 192.0.[0,2].*
DENY 192.88.99.*
DENY 192.168.*
DENY 198.[18,19].*
DENY 198.51.100.*
DENY 203.0.113.*
DENY [224-239].*
DENY 233.252.0.*
DENY [240-255].*
DENY 255.255.255.255

DEFAULT ALLOW
```

First alpha version:
**Yes:**
- ~~Screen in/output~~
- ~~Goodize events~~
    - ~~paste event~~
    - ~~en-/disable machine events~~
- Rendering framework
- Filesystem
- Bootloader support
- IO Net (so screens can be independent)
    - Addon mod api
    - Redstone interaction
- Tiers
- Basic OS
- Proper saving and loading
- Crafting recipes

**Maybe:**
- Dyable keycards lua integration
    - Basic data-storing keycard (read/write data, maybe with read-only lock)
    - Advanced keycard (set ecdsa key and then perform operations, maybe with read-only lock)
- Inventory reading

**No:**
- Networking
    - Top level services
- Hand held computer
- Sounds
- Servers








8bit color rgb-332
https://en.wikipedia.org/wiki/List_of_8-bit_computer_hardware_graphics#8-bit_RGB_palettes

