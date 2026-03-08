package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.lua.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.lua.components.fs.LuaFsFileUD;
import dev.asdf00.mc.advcomp.lua.components.fs.ManagedStorageHandler;
import dev.asdf00.mc.advcomp.lua.components.fs.VirtualFile;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Stream;

public class ManagedMassStorageUD extends BaseMassStorageUD {
    private final int totalCapacityBytes;
    private int diskStorageId = -1;
    private ManagedStorageHandler fs = null;


    /**
     * Use this for the initial creation only. For the deserialization mainly use {@link #luaDeserialize(LuaObject[], ByteArrayReader, Queue, Object)} instead.
     */
    public static ManagedMassStorageUD initFromItemStack(String storageFamilyName, int totalCapacityBytes) {
        return new ManagedMassStorageUD(storageFamilyName,  totalCapacityBytes);
    }

    /**
     * Use this for the initial creation only. For the deserialization mainly use {@link #luaDeserialize(LuaObject[], ByteArrayReader, Queue, Object)} instead.
     */
    protected ManagedMassStorageUD(String storageFamilyName, int totalCapacityBytes) {
        super(storageFamilyName, "managed");
        this.totalCapacityBytes = totalCapacityBytes;
    }

    private ManagedMassStorageUD(LuaVirtualMachine acVm, boolean isAccessible, String storageFamilyName, int totalCapacityBytes, ManagedStorageHandler fs) {
        super(acVm, isAccessible, storageFamilyName, "managed");
        this.totalCapacityBytes = totalCapacityBytes;
        this.fs = fs;
    }

    /**
     * Is supposed to only run once during object construction. NOT during deserialization
     */
    @Override
    public void onVmInit(LuaVirtualMachine acVm, ItemStack is) {
        super.onVmInit(acVm, is);
        initItemStackDiskIdIfNeeded(is);
        initFilesystem(is);
    }

    public void initFilesystem(ItemStack is) {
        if (fs != null)
            throw new RuntimeException("Fs is already inited for disk id %d but was attempted to be initialized again.".formatted(diskStorageId));

        initItemStackDiskIdIfNeeded(is);


        assert is.getTag() != null;
        diskStorageId = is.getTag().getInt("mDiskId");
        fs = new ManagedStorageHandler(diskStorageId);
    }

    public static void initItemStackDiskIdIfNeeded(ItemStack is) {
        var tag = is.getOrCreateTag();
        if (!tag.contains("mDiskId")) { // no folder associated yet with this disk
            int newDiskId = AdvancedComputers.globalDataStorage.getNextFreeUniqueStorageId();
            tag.putInt("mDiskId", newDiskId);
        }
    }

    @LuaCallable
    public LuaObject open(LuaObject[] args) {
        if (args.length >= 1) {
            var fileName = args[0];
            if (!fileName.isString()) {
                throw new LuaJavaError("Second argument must be string but was %s".formatted(fileName.getTypeAsString()));
            }
            if (args.length == 1) {
                return openInner(fileName.asString(), false);
            } else if (args.length == 2) {
                var autoCreate = args[1];
                if (!autoCreate.isBoolean()) {
                    throw new LuaJavaError("Third argument must be boolean but was %s".formatted(autoCreate.getTypeAsString()));
                }
                return openInner(fileName.asString(), autoCreate.getBool());
            }
        }
        throw new LuaJavaError("Expected 3 arguments but got %s".formatted(args.length + 1));
    }

    public LuaObject openInner(String fileNameL, boolean autoCreate) {
        var fileName = fileNameL;
        if (fileName.startsWith("/"))
            fileName = fileName.substring(1);

        if (fileName.endsWith("/")) {
            throw new LuaJavaError("Filename cannot end with a slash");
        }

        if (fs.directoryExists(fileName))
            throw new LuaJavaError("Path points to a directory");

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
        return LuaObject.of(new LuaFsFileUD(fs.getOrCreateFile(fileName), this));
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
        // traverse the chain and see if any parent folder name is already taken by a file, which would be illegal
        var segments = path.split("/");
        var currentFilePath = new StringBuilder(path.length());
        for (int i = 0; i < segments.length; i++) {
            currentFilePath.append('/').append(segments[i]);
            if (fs.fileExists(currentFilePath.toString()))
                throw new LuaJavaError("Unable to create directory or parents: a directory name is already in use by a file");
        }

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

        if (fs.directoryExists(dest)) {
            throw new LuaJavaError("Destination path is a directory");
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
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return new ByteArrayBuilder()
                .append(isAccessible)
                .append(diskStorageId)
                .append(storageFamilyName)
                .append(totalCapacityBytes)
                .toArray();
    }

    @LuaDeserializer
    public static ManagedMassStorageUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var isAccessible = reader.readBool();
        var diskStorageId = reader.readInt();
        var storageFamilyName = reader.readString();
        var totalCapacityBytes = reader.readInt();

        return new ManagedMassStorageUD((LuaVirtualMachine) additionalData, isAccessible, storageFamilyName,
                totalCapacityBytes, new ManagedStorageHandler(diskStorageId));
    }

    @Override
    int getDiskId() {
        return diskStorageId;
    }

    public String serializeVirtualFile(VirtualFile f) {
        return f.getFilepathWithinFs().toString();
    }

    public VirtualFile deserializeVirtualFile(String fData) {
        try {
            return (VirtualFile) (openInner(fData, false).refVal);
        } catch (LuaJavaError ex) {
            throw new RuntimeException("Deserialization failed as we somehow are not able to open a file that used to exist when we serialized the vm", ex);
        }
    }
}
