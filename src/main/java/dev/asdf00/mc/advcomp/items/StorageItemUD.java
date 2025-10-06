package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.lua.components.fs.LuaFsFileUD;
import dev.asdf00.mc.advcomp.lua.components.fs.ManagedStorageHandler;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class StorageItemUD extends Item implements LuaUserDataComponent {
    @Override
    public String getComponentType() {
        return "disk";
    }

    private final int totalCapcityBytes;
    private final int diskStorageId;
    private final boolean IsUnmanaged = false;
    //    private UnmanagedStorageHandler storageComponent;
    private ManagedStorageHandler fs = null;

    private ManagedStorageHandler getFs() {
        if (fs == null)
            fs = new ManagedStorageHandler(diskStorageId);
        return fs;
    }

    public StorageItemUD(int totalCapcityBytes) {
        super(new Properties());
        this.totalCapcityBytes = totalCapcityBytes;
//        storageComponent = new UnmanagedStorageHandler("someid", totalCapcityBytes);
        diskStorageId = 0;
    }

    private int diskSlotId = -1;

    public void setInventorySlot(int diskSlotId) {
        this.diskSlotId = diskSlotId;
    }

    @LuaExposed(LuaExposed.Policy.READ)
    public final LuaProperty diskSlot = LuaProperty.ofInt(
            () -> diskSlotId,
            null
    );

    @LuaCallable
    public LuaObject open(LuaObject[] args) {
        if (args.length >= 1) {
            var fileName = args[0];
            if (!fileName.isString()) {
                throw new LuaJavaError("Second argument must be string but was %s".formatted(fileName.getTypeAsString()));
            }
            if (args.length == 1) {
                return open(fileName.asString(), false);
            } else if (args.length == 2) {
                var autoCreate = args[1];
                if (!autoCreate.isBoolean()) {
                    throw new LuaJavaError("Third argument must be boolean but was %s".formatted(autoCreate.getTypeAsString()));
                }
                return open(fileName.asString(), autoCreate.getBool());
            }
        }
        throw new LuaJavaError("Expected 3 arguments but got %s".formatted(args.length + 1));
    }

    //@LuaCallable
    public LuaObject open(String fileNameL, boolean autoCreate) {
        var fileName = fileNameL;
        if (fileName.startsWith("/"))
            fileName = fileName.substring(1);

        if (fileName.endsWith("/")) {
            throw new LuaJavaError("Filename cannot end with a slash");
        }

        var fileExists = getFs().fileExists(fileName);
        if (!fileExists) {
            if (!autoCreate) { // if no autocreate and doesnt exist, then we throw an error
                throw new LuaJavaError("File '%s' does not exist".formatted(fileName));
            } else { // if we will be creating a new file, check the existence of the parent folder
                var lastSlash = fileName.lastIndexOf('/');
                if (lastSlash != -1) { // only care about paths that contain a slash, otherwise this is in the root folder
                    var folderPath = fileName.substring(0, lastSlash);
                    if (getFs().getDirectory(folderPath) == null) {
                        throw new LuaJavaError("Parent folder '%s' of '%s' does not exist".formatted(folderPath, fileName));
                    }
                }
            }
        }
        return LuaObject.of(new LuaFsFileUD(getFs().getOrCreateFile(fileName)));
    }

    @LuaCallable
    public LuaObject list(String path) {
        var files = getFs().getFilesInDirectory(path);
        var dirs = getFs().getDirectoriesInDirectory(path);
        return LuaObject.tableFromArray(Stream.concat(files.stream(), dirs.stream().map(x -> x + "/")).map(LuaObject::of).toArray(LuaObject[]::new));
    }

    @LuaCallable
    public boolean fileExists(String path) {
        return getFs().getFileOrNull(path) != null;
    }

    @LuaCallable
    public boolean directoryExists(String path) {
        return getFs().getDirectory(path) != null;
    }

    @LuaCallable
    public void makeDirectory(String path) {
        getFs().createDirectoryAndParents(path);
    }

    @LuaCallable
    public boolean delete(String path) {
        if (!getFs().fileExists(path)) {
            throw new LuaJavaError("File '%s' does not exist".formatted(path));
        }
        return getFs().tryDeleteFile(path); // TODO isnt this always true?
    }

    @LuaCallable
    public void copy(String src, String dest) {
        if (!getFs().fileExists(src)) {
            throw new LuaJavaError("File '%s' does not exist".formatted(src));
        }
        getFs().getOrCreateFile(dest).writeAllText(getFs().getFileOrNull(src).readAllText());
    }

    @LuaCallable
    public void move(String src, String dest) {
        copy(src, dest);
        if (!getFs().tryDeleteFile(src)) {
            throw new IllegalStateException("somehow file deletion failed after copying");
        }
    }

    @LuaCallable
    public int getSize(String path) {
        if (!getFs().fileExists(path)) {
            throw new LuaJavaError("File '%s' does not exist".formatted(path));
        }
        return getFs().getFileOrNull(path).readAllText().length();
    }


    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static StorageItemUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}
