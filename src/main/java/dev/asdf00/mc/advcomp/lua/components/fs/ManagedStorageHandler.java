package dev.asdf00.mc.advcomp.lua.components.fs;

import dev.asdf00.mc.advcomp.types.RuntimeAssert;
import dev.asdf00.mc.advcomp.utils.AcPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

public class ManagedStorageHandler implements StorageHandler {
    private final int diskId;
    private final DirectoryNode root;
    boolean isReadOnly = false;

    @Override
    public void DeleteAllData() {

    }

    public static String encodeFilename(String filename) {
        var rv = new StringBuilder(filename.length() * 2);
        for (int i = 0; i < filename.length(); i++) {
            var c = filename.charAt(i);
            if (Character.isUpperCase(c)) {
                rv.append('=').append(Character.toLowerCase(c));
            } else if (c == '=') {
                rv.append("==");
            } else {
                rv.append(c);
            }
        }
        return rv.toString();
    }

    public static String decodeFilename(String filename) {
        var rv = new StringBuilder(filename.length());
        for (int i = 0; i < filename.length(); i++) {
            var c = filename.charAt(i);
            RuntimeAssert.RuntimeAssert(!Character.isUpperCase(c), "expected no uppercase chars in %s".formatted(filename));
            if (c != '=') {
                rv.append(c);
            } else {
                i++;
                var c2 = filename.charAt(i);
                if (c2 == '=') {
                    rv.append("=");
                } else {
                    RuntimeAssert.RuntimeAssert(Character.isLowerCase(c2), "expected lowercase char after = but got %s".formatted(c2));
                    rv.append(Character.toUpperCase(c2));
                }
            }
        }
        return rv.toString();
    }

    public ManagedStorageHandler(int diskId) {
        this.diskId = diskId;
        root = new DirectoryNode(AcPaths.getManagedDiskFolderPath(diskId).toString(), null, isReadOnly);
        try {
            Files.createDirectories(root.getRealDiskPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            return Files.list(dirPath).filter(x -> !Files.isDirectory(x)).map(x -> x.getFileName().toString()).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<String> getDirectoriesInDirectory(String path) {
        var dirPath = root.getDirectory(trimPath(path)).getRealDiskPath();
        try (var s = Files.list(dirPath)) {
            return s.filter(Files::isDirectory).map(x -> x.getFileName().toString()).toList();
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
