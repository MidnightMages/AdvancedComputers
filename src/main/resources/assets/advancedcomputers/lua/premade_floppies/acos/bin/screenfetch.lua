-- src: https://emojicombos.com/bongo-cat-ascii-art; bongos removed

local bongoLines = {}
local function bongo(bText)
    table.insert(bongoLines, bText)
end
local infoLines = {}
local function info(i)
    table.insert(infoLines, i)
end

bongo("                               █                                      ")
bongo("                             ██ ██                                    ")
bongo("                           ██    ██                                   ")
bongo("                      █████        █████                              ")
bongo("                   ███                 █████                          ")
bongo("                ███                         █████                     ")
bongo("      ██████  ███                                ███       ████       ")
bongo("     █  █   ██                                      ████████ ██       ")
bongo("    ██    ██  █                                              ██       ")
bongo("    ██ ████           ███                                    █        ")
bongo("    ██   █            ███                                   ██        ")
bongo("██████                                                     ██         ")
bongo("     ████████             █             ███                ██         ")
bongo("             ███████       ███████      ███                ██         ")
bongo("                    ████████                                 ██       ")
bongo("                            █████████                         ██      ")
bongo("                                     ███████                  ██      ")
bongo("                                          ██                   ██     ")
bongo("                                         ██         █████████   █     ")
bongo("                                         ██      ███        ████████  ")
bongo("                                         ██   ████                  ██")
bongo("                                           ████                       ")
-- src: https://www.asciiart.eu/image-to-ascii

-- linux screenfetch info:
info("root@hostname")
info("OS: Advanced OS")
info("Kernel: Kernelname and version")
info("Uptime: 0m")
info("Packages: 0")
info("Shell: BongoShell")
info("Disk: 1MiB/10MiB (10%)")
info("CPU: ???")
info("RAM: 16MiB / 64MiB")

for i = 1, math.max(#bongoLines, #infoLines) do
    print((bongoLines[i] or "") .. "  " .. (infoLines[i] or ""))
end
