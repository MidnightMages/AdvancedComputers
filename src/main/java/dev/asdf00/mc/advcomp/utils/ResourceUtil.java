package dev.asdf00.mc.advcomp.utils;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.lua.LuaMain;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;

public class ResourceUtil {
    public static String loadLuaScript(String name) {
        try (var stream = LuaMain.class.getClassLoader().getResourceAsStream("assets/advancedcomputers/lua/" + name)) {
            Objects.requireNonNull(stream, "Error reading resource '%s'".formatted(name));
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Resource '%s' not found!".formatted(name));
        }
    }

    public static void copyPremadeFloppyIntoManagedDiskFolder(String floppyType, int managedDiskId) {
        // TODO test if this works when the mod is running outside the dev environment
        var modFile = ModList.get().getModFileById(AdvancedComputers.MODID).getFile();
        var srcFolder = modFile.findResource("assets/advancedcomputers/lua/premade_floppies/%s".formatted(floppyType));
        if (!Files.exists(srcFolder))
            return;
        var destFolder = AcPaths.getManagedDiskFolderPath(managedDiskId);
        try {
            MiscUtil.copyFolderRecursively(srcFolder, destFolder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
