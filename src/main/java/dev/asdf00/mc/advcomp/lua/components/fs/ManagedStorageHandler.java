package dev.asdf00.mc.advcomp.lua.components.fs;

import dev.asdf00.mc.advcomp.AdvancedComputers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

public class ManagedStorageHandler implements StorageHandler {
    private final int diskId;
    private final DirectoryNode root;
    boolean isReadOnly = false;

    @Override
    public void DeleteAllData() {

    }

    public ManagedStorageHandler(int diskId) {
        this.diskId = diskId;
        root = new DirectoryNode(getDiskStorageFolder().toString(), null, isReadOnly);
        try {
            Files.createDirectories(root.getRealDiskPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getDiskStorageFolder() {
        return AdvancedComputers.getAcWorldSaveSubFolder().resolve("managed/%s/".formatted(diskId));
    }

    private static String trimPath(String s) {
        while (s.endsWith("/"))
            s = s.substring(0, s.length() - 1);
        return s.startsWith("/") ? s.substring(1) : s;
    }

    public VirtualFile getFileOrNull(String s) {
        return root.getFileOrNull(trimPath(s));
    }

    public VirtualFile getOrCreateFile(String s) {
        return root.getOrCreateFile(trimPath(s));
    }

    public DirectoryNode getDirectory(String s) {
        return root.getDirectory(trimPath(s));
    }

    public DirectoryNode createDirectoryAndParents(String s) {
        return root.createDirectoryAndParents(trimPath(s));
    }

    public Collection<String> getFilesInDirectory(String path) {
        var dirPath = root.getDirectory(trimPath(path)).getRealDiskPath();
        try {
            return Files.list(dirPath).filter(x->!Files.isDirectory(x)).map(Path::toString).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<String> getDirectoriesInDirectory(String path) {
        var dirPath = root.getDirectory(trimPath(path)).getRealDiskPath();
        try (var s = Files.list(dirPath)) {
            return s.filter(Files::isDirectory).map(Path::toString).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean fileExists(String path) {
        return getFileOrNull(path) != null;
    }

    public boolean tryDeleteFile(String s) {
        var f = root.getFileOrNull(trimPath(s));
        if (f != null) {
            f.delete();
            return true;
        }
        return false;
    }
}
