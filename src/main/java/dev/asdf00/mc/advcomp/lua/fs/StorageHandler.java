package dev.asdf00.mc.advcomp.lua.fs;

import java.nio.file.Path;

public interface StorageHandler {

    /**
     * Deletes all data associated with this storage container. E.g. when the corresponding item is destroyed
     */
    void CleanupAllData();
}

