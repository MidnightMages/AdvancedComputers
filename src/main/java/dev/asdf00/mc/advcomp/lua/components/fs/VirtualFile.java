package dev.asdf00.mc.advcomp.lua.components.fs;

import dev.asdf00.jluavm.exceptions.LuaJavaError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class VirtualFile {
    private final DirectoryNode parentFolder;
    private final String fileName;

    public VirtualFile(String fileName, DirectoryNode parentFolder) {
        this.parentFolder = parentFolder;
        this.fileName = fileName;
    }

    private Path getRealDiskPath() {
        var path = parentFolder.getRealDiskPath().resolve(this.fileName);
        var rootDir = parentFolder.getFsRootPath();
        if (!path.toAbsolutePath().startsWith(rootDir.toAbsolutePath()))
            throw new RuntimeException("Why are we trying to write outside of our root path?");
        return path;
    }

    public void writeAllText(String s) {
        if (!parentFolder.isPhysReadOnly) {
            throw new LuaJavaError("Filesystem is readonly!");
        }
        try {
            Files.writeString(getRealDiskPath(), s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void appendAllText(String s) {
        writeAllText(readAllText() + s);
    }

    public String readAllText() {
        try {
            return Files.readString(getRealDiskPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        try {
            Files.delete(getRealDiskPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
