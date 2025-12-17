package dev.asdf00.mc.advcomp.utils;

import dev.asdf00.mc.advcomp.AdvancedComputers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AcPaths {
    public static Path getAcWorldSaveSubFolderPath() {
        return AdvancedComputers.getAcWorldSaveSubFolder();
    }

    public static Path getManagedDiskFolderPath(int diskId) {
        return getAcWorldSaveSubFolderPath().resolve("managed/%s/".formatted(diskId));
    }

    public static Path getUnmanagedDiskFilePath(int storageId) {
        return getAcWorldSaveSubFolderPath().resolve(Path.of("unmanaged", storageId + ".bin"));
    }

    public static Path getUefiFilePath(int uefiId) {
        return getAcWorldSaveSubFolderPath().resolve("uefi/%s.lua".formatted(uefiId));
    }

    public static void createPathsIfNecessary() {
        var dirsToCreate = "managed;unmanaged;uefi";
        var base = getAcWorldSaveSubFolderPath();
        for (var d : dirsToCreate.split(";")) {
            try {
                Files.createDirectories(base.resolve(d));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
