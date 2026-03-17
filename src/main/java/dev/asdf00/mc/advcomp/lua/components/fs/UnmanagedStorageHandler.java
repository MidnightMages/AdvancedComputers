package dev.asdf00.mc.advcomp.lua.components.fs;

import com.mojang.logging.LogUtils;
import dev.asdf00.mc.advcomp.utils.AcPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

public class UnmanagedStorageHandler implements StorageHandler {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final int capacity;
    private static final int headerLength = 0; // TODO 1 byte saveformat version; 4 byte data file size
    private final int storageId;

    public UnmanagedStorageHandler(int storageId, int capacity) {
        this.capacity = capacity;
        this.storageId = storageId;
    }

    private Path getSaveFilePath() {
        return AcPaths.getUnmanagedDiskFilePath(storageId);
    }

    public void writeToPosition(int position, byte[] data) {
        try {
            var f = new RandomAccessFile(getSaveFilePath().toFile(), "rw");
            f.write(data, position+headerLength, data.length);
            f.close();
        }
        catch (IOException e) {
            LOGGER.info("[UnmanagedFilesystem] Caught exception on write: %s".formatted(e));
        }
    }

    public byte[] readFromPosition(int position, int length) {
        try {
            var f = new RandomAccessFile(getSaveFilePath().toFile(), "r");
            // TODO handle oob reads
            var data = new byte[length];
            f.readFully(data, position+headerLength, data.length);
            return data;
        }
        catch (IOException e) {
            LOGGER.info("[UnmanagedFilesystem] Caught exception on write: %s".formatted(e));
        }
        // todo throw exception;
        return null;
    }

    public void fill(int startPosition, int size, byte value) {

    }

    public void erase() {
        fill(0, this.capacity, (byte) 0);
    }

    @Override
    public void DeleteAllData() {
        try {
            Files.delete(getSaveFilePath());
        }
        catch (IOException e) {
            LOGGER.info("[UnmanagedFilesystem] Caught exception on cleanup: %s".formatted(e));
        }
    }
}
