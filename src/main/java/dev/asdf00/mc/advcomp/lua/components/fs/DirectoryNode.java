package dev.asdf00.mc.advcomp.lua.components.fs;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DirectoryNode {
    final String nameOrPath;
    private final DirectoryNode parentFolder;
    final boolean isPhysReadOnly;

    public DirectoryNode(String nameOrPath, DirectoryNode parentFolder, boolean isPhysReadOnly) {
        this.nameOrPath = nameOrPath;
        this.parentFolder = parentFolder;
        this.isPhysReadOnly = isPhysReadOnly;
    }

    private DirectoryNode getLocalChildDirOrNull(String dirName) {
        var p = getRealDiskPath().resolve(dirName);
        if (Files.isDirectory(p)) {
            return new DirectoryNode(dirName, this, isPhysReadOnly);
        }
        return null;
    }

    private VirtualFile getLocalFileOrNull(String fileName) {
        var p = getRealDiskPath().resolve(fileName);
        if (Files.isRegularFile(p)) {
            return new VirtualFile(fileName, this);
        }
        return null;
    }

    public VirtualFile getFileOrNull(String s) {
        var splitted = s.split("/", 2);
        return splitted.length == 1 ?
                getLocalFileOrNull(splitted[0]) :
                Optional.ofNullable(getLocalChildDirOrNull(splitted[0])).map(x -> x.getFileOrNull(splitted[1])).orElse(null);
    }

    public VirtualFile getOrCreateFile(String s) {
        var splitted = s.split("/", 2);
        var fileObject = splitted.length == 1 ?
                getFileOrNull(splitted[0]) :
                Optional.ofNullable(getLocalChildDirOrNull(splitted[0])).map(x -> x.getOrCreateFile(splitted[1])).orElse(null);
        if (fileObject == null) {
            fileObject = new VirtualFile(splitted[0], this);
            fileObject.writeAllText("");
        }
        return fileObject;
    }

    public DirectoryNode getDirectory(String s) {
        if (s.isEmpty())
            return this;

        var splitted = s.split("/", 2);
        return splitted.length == 1 ?
                getLocalChildDirOrNull(splitted[0]) :
                Optional.ofNullable(getLocalChildDirOrNull(splitted[0])).map(x -> x.getDirectory(splitted[1])).orElse(null);
    }

    public DirectoryNode createDirectoryAndParents(String s) {
        if (s.isEmpty())
            return this;

        var splitted = s.split("/", 2);
        var childDir = getLocalChildDirOrNull(splitted[0]);
        if (childDir == null) {
            childDir = new DirectoryNode(splitted[0], this, isPhysReadOnly);
            try {
                Files.createDirectory(getRealDiskPath().resolve(splitted[0]));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return splitted.length == 1 ? childDir : childDir.createDirectoryAndParents(splitted[1]);
    }

    public Path getRealDiskPath() {
        if (parentFolder == null)
            return Path.of(this.nameOrPath);

        return parentFolder.getRealDiskPath().resolve(this.nameOrPath);
    }

    public Path getFsRootPath() {
        return parentFolder == null ? Path.of(this.nameOrPath) : parentFolder.getFsRootPath();
    }
}
