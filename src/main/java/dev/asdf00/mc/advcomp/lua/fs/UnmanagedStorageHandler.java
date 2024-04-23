package dev.asdf00.mc.advcomp.lua.fs;

import com.mojang.logging.LogUtils;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.lua.AcLuaFunction;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class UnmanagedStorageHandler implements StorageHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path suffix;
    private final int capacity;
    private static final int headerLength = 5; // 1 byte saveformat version; 4 byte data file size

    public UnmanagedStorageHandler(String storageId, int capacity) {
        this.capacity = capacity;
        suffix = Path.of("unmanaged", storageId + ".bin");
    }

    private Path getSaveFilePath() {
        return AdvancedComputers.getAcWorldSaveSubFolder().resolve(suffix);
    }

    @AcLuaFunction(functionName = "writeToPosition", doc = "function(int position, int length):byte[] data; Reads the specified range of bytes from this storage medium.")
    public void writeToPosition(int position, byte[] data) {
        try {
            var f = new RandomAccessFile(getSaveFilePath().toFile(), "rw");
            f.write(data, position, data.length);
            f.close();
        }
        catch (IOException e) {
            LOGGER.info("[UnmanagedFilesystem] Caught exception on write: %s".formatted(e));
        }
    }

    @AcLuaFunction(functionName = "readFromPosition", doc = "function(int position, int length):byte[] data; Reads the specified range of bytes from this storage medium.")
    public byte[] readFromPosition(int position, int length) {
        try {
            var f = new RandomAccessFile(getSaveFilePath().toFile(), "r");
            // TODO handle oob reads
            var data = new byte[length];
            f.readFully(data, position, data.length);
            return data;
        }
        catch (IOException e) {
            LOGGER.info("[UnmanagedFilesystem] Caught exception on write: %s".formatted(e));
        }
        // todo throw exception;
        return null;
    }

    @AcLuaFunction(functionName = "fill", doc = "function(int:startPosition, int:size, byte:value):void; Fills the entire storage medium with the specified value.")
    public void fill(int startPosition, int size, byte value) {

    }

    @AcLuaFunction(functionName = "erase", doc = "function():void; Erases all data on the storage medium.")
    public void erase() {
        fill(0, this.capacity, (byte) 0);
    }

    @Override
    public void CleanupAllData() {
        try {
            Files.delete(getSaveFilePath());
        }
        catch (IOException e) {
            LOGGER.info("[UnmanagedFilesystem] Caught exception on cleanup: %s".formatted(e));
        }
    }
}
