package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.api.userdata.LuaProperty;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.lua.components.fs.LuaFsFileUD;
import dev.asdf00.mc.advcomp.lua.components.fs.ManagedStorageHandler;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DiskItemUD implements LuaUserDataComponent {

    public DiskItemUD(ItemStack is) {
        var tag = is.getOrCreateTag();

        if (!tag.contains("mDiskId")) { // no folder associated yet with this disk
            int newDiskId = AdvancedComputers.globalDataStorage.getNextFreeManagedDiskId();
            tag.putInt("mDiskId", newDiskId);
        }

        if(fs == null)
            initFilesystem(tag.getInt("mDiskId"));
    }

    @Override
    public String getComponentType() {
        return "disk";
    }

    //    private UnmanagedStorageHandler storageComponent;
    private int diskStorageId = -1;
    private ManagedStorageHandler fs = null;

    public void initFilesystem(int diskId) {
        diskStorageId = diskId;
        if(fs != null)
            throw new RuntimeException("Fs is already inited for disk id "+diskId);

        fs = new ManagedStorageHandler(diskStorageId);
    }

    private int diskSlotId = -1;

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

        var fileExists = fs.fileExists(fileName);
        if (!fileExists) {
            if (!autoCreate) { // if no autocreate and doesnt exist, then we throw an error
                throw new LuaJavaError("File '%s' does not exist".formatted(fileName));
            } else { // if we will be creating a new file, check the existence of the parent folder
                var lastSlash = fileName.lastIndexOf('/');
                if (lastSlash != -1) { // only care about paths that contain a slash, otherwise this is in the root folder
                    var folderPath = fileName.substring(0, lastSlash);
                    if (fs.getDirectory(folderPath) == null) {
                        throw new LuaJavaError("Parent folder '%s' of '%s' does not exist".formatted(folderPath, fileName));
                    }
                }
            }
        }
        return LuaObject.of(new LuaFsFileUD(fs.getOrCreateFile(fileName)));
    }

    @LuaCallable
    public LuaObject list(String path) {
        var files = fs.getFilesInDirectory(path);
        var dirs = fs.getDirectoriesInDirectory(path);
        return LuaObject.tableFromArray(Stream.concat(files.stream(), dirs.stream().map(x -> x + "/")).map(LuaObject::of).toArray(LuaObject[]::new));
    }

    @LuaCallable
    public boolean fileExists(String path) {
        return fs.getFileOrNull(path) != null;
    }

    @LuaCallable
    public boolean directoryExists(String path) {
        return fs.getDirectory(path) != null;
    }

    @LuaCallable
    public void makeDirectory(String path) {
        fs.createDirectoryAndParents(path);
    }

    @LuaCallable
    public boolean delete(String path) {
        if (!fs.fileExists(path)) {
            throw new LuaJavaError("File '%s' does not exist".formatted(path));
        }
        return fs.tryDeleteFile(path); // TODO isnt this always true?
    }

    @LuaCallable
    public void copy(String src, String dest) {
        if (!fs.fileExists(src)) {
            throw new LuaJavaError("File '%s' does not exist".formatted(src));
        }
        fs.getOrCreateFile(dest).writeAllText(fs.getFileOrNull(src).readAllText());
    }

    @LuaCallable
    public void move(String src, String dest) {
        copy(src, dest);
        if (!fs.tryDeleteFile(src)) {
            throw new IllegalStateException("somehow file deletion failed after copying");
        }
    }

    @LuaCallable
    public int getSize(String path) {
        if (!fs.fileExists(path)) {
            throw new LuaJavaError("File '%s' does not exist".formatted(path));
        }
        return fs.getFileOrNull(path).readAllText().length();
    }


    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs) {
        // TODO actually provide serializaion
        return null;
    }

    @LuaDeserializer
    public static DiskItemUD todoDeserializer(LuaObject[] objs, ByteArrayReader reader) {
        // TODO actually provide serializaion
        return null;
    }
}
