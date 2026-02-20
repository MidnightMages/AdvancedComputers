package dev.asdf00.mc.advcomp.lua.components.fs;

import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.types.RuntimeAssert;
import dev.asdf00.mc.advcomp.utils.AcPaths;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

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

    public static String decodeFilenameOrNull(String filename) {
        var rv = new StringBuilder(filename.length());
        for (int i = 0; i < filename.length(); i++) {
            var c = filename.charAt(i);
            if (Character.isUpperCase(c)) {
                AdvancedComputers.LOGGER.warn("Found uppercase char in existing filename '%s'.".formatted(filename));
                return null;
            }
            if (c != '=') {
                rv.append(c);
            } else {
                i++;
                var c2 = filename.charAt(i);
                if (c2 == '=') {
                    rv.append("=");
                } else {
                    if (!Character.isLowerCase(c2)) {
                        AdvancedComputers.LOGGER.warn("Expected lowercase char after = in '%s'.".formatted(filename));
                        return null;
                    }
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
            var rootPath =root.getRealDiskPath();
            Files.createDirectories(rootPath);
            ArrayList<Path> pathsToFix = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(rootPath)){
                stream.forEach(path -> {
                    // check if we can correctly decode the current filename (folder or file, but only last segment of the path)
                    var relPath = rootPath.relativize(path);
                    var lastSegment = relPath.getName(relPath.getNameCount()-1);
                    if (decodeFilenameOrNull(lastSegment.toString()) == null) // if we cannot, track it
                        pathsToFix.add(relPath);
                });
            }
            pathsToFix.sort(Comparator.comparing(Path::getNameCount, Comparator.reverseOrder()));
            // start with the longest paths, as we need to fix nested sub-elements first, e.g. for /correct/broken/broken2
            // we would need to fix broken2 first and then broken as we lose track of broken2 otherwise
            for (var relativePath : pathsToFix) {
                Path absolutePathToFix = rootPath.resolve(relativePath);
                Path parentPath = absolutePathToFix.getParent();
                Path lastSegment = absolutePathToFix.getName(absolutePathToFix.getNameCount()-1);
                String fixedLastSegment = encodeFilename(lastSegment.toString());
                Path fixedPath = parentPath.resolve(fixedLastSegment);
                if (Files.isDirectory(absolutePathToFix) || Files.isRegularFile(absolutePathToFix)) {
                    Files.move(absolutePathToFix, fixedPath);
                } else{
                    throw new RuntimeException("Somehow a path we wanted to fix is neither a file nor a directory?");
                }
            }

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
            return Files.list(dirPath).filter(x -> !Files.isDirectory(x)).map(x -> DirectoryNode.decodeFilename(x.getFileName().toString())).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Collection<String> getDirectoriesInDirectory(String path) {
        var dirPath = root.getDirectory(trimPath(path)).getRealDiskPath();
        try (var s = Files.list(dirPath)) {
            return s.filter(Files::isDirectory).map(x -> DirectoryNode.decodeFilename(x.getFileName().toString())).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean fileExists(String path) {
        return getFileOrNull(path) != null;
    }

    public boolean directoryExists(String path) {
        return this.getDirectory(path) != null;
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
