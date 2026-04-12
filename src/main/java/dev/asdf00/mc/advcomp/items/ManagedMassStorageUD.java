package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaExposed;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.AdvancedComputers;
import dev.asdf00.mc.advcomp.Config;
import dev.asdf00.mc.advcomp.lua.vm.LuaVirtualMachine;
import dev.asdf00.mc.advcomp.utils.AcPaths;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class ManagedMassStorageUD extends BaseMassStorageUD {
    @LuaExposed(LuaExposed.Policy.READ)
    private final int totalCapacityBytes;
    private int diskStorageId = -1;

    // ## dont serialize ##
    private boolean isInited = false;
    private Path _dont_use_directly_fsRealBasePath = null;
    private final HashMap<File, LuaFsFileUD> openFilehandles = new HashMap<>(); // will be populated by the filehandles automatically

    private Path getFsRealBasePath() {
        RuntimeAssert.RuntimeAssert(isInited, "somehow fs is not inited yet");
        return _dont_use_directly_fsRealBasePath; // fine here because we check isInited
    }
    // ####################

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

    /**
     * This ctor is for deserialization
     */
    private ManagedMassStorageUD(LuaVirtualMachine acVm, boolean isAccessible, String storageFamilyName, int totalCapacityBytes, int diskStorageId) {
        super(acVm, isAccessible, storageFamilyName, "managed");
        this.totalCapacityBytes = totalCapacityBytes;
        setBasePathAndConvertFs(diskStorageId);
    }

    // ############################### SETUP ###############################

    /**
     * Is supposed to only run once during object construction. NOT during deserialization
     */
    @Override
    public void onVmInit(LuaVirtualMachine acVm, ItemStack is) {
        super.onVmInit(acVm, is);

        if (isInited)
            throw new RuntimeException("Fs is already inited for disk id %d but was attempted to be initialized again.".formatted(diskStorageId));

        initItemStackDiskIdIfNeeded(is);

        assert is.getTag() != null;
        setBasePathAndConvertFs(is.getTag().getInt("mDiskId"));
    }

    private void setBasePathAndConvertFs(int diskId) {
        diskStorageId = diskId;
        var rootPath =  AcPaths.getManagedDiskFolderPath(diskStorageId);
        this._dont_use_directly_fsRealBasePath = rootPath;
        try {
            Files.createDirectories(rootPath);
            if (Config.escapeUppercaseCharactersOnHost) {
                ArrayList<Path> pathsToFix = new ArrayList<>();
                try (Stream<Path> stream = Files.walk(rootPath)) {
                    stream.forEach(path -> {
                        // check if we can correctly decode the current filename (folder or file, but only last segment of the path)
                        var relPath = rootPath.relativize(path);
                        var lastSegment = relPath.getName(relPath.getNameCount() - 1);
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
                    Path lastSegment = absolutePathToFix.getName(absolutePathToFix.getNameCount() - 1);
                    String fixedLastSegment = encodeFilename(lastSegment.toString());
                    Path fixedPath = parentPath.resolve(fixedLastSegment);
                    if (Files.isDirectory(absolutePathToFix) || Files.isRegularFile(absolutePathToFix)) {
                        Files.move(absolutePathToFix, fixedPath);
                    } else {
                        throw new RuntimeException("Somehow a path we wanted to fix is neither a file nor a directory?");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        isInited = true;
    }

    public static void initItemStackDiskIdIfNeeded(ItemStack is) {
        var tag = is.getOrCreateTag();
        if (!tag.contains("mDiskId")) { // no folder associated yet with this disk
            int newDiskId = AdvancedComputers.globalDataStorage.getNextFreeUniqueStorageId();
            tag.putInt("mDiskId", newDiskId);
        }
    }

    // ########################## LUA API RELATED ##########################
    private static final HashSet<String> supportedBaseFileModes = new HashSet<>(List.of(new String[]{
            "r", "w", "a", "r+", "w+"//, "rb", "wb", "ab", "r+b", "w+b"
    }));

    @LuaCallable
    public LuaFsFileUD open(String filePath) {
        return open(filePath, "r");
    }

    @LuaCallable
    public LuaFsFileUD open(String filePath, String mode) {
        // error on slash at the end
        if (isLuaPathDirectory(filePath))
            throw new LuaJavaError("Expected file path, but path ended with a slash.");

        filePath = normalizeEncodeAbsFilePath(filePath);
        if (!supportedBaseFileModes.contains(mode)) {
            throw new LuaJavaError("Unsupported file mode '%s'".formatted(mode));
        }

        if (Files.isDirectory(getRealFsPath(filePath)))
            throw new LuaJavaError("file '%s' is a directory".formatted(filePath));

        var baseMode = mode.charAt(0);
        boolean isPlusMode = mode.contains("+");
        LuaFsFileUD handle;
        switch (baseMode) {
            case 'r' -> {
                if (!fileExists(filePath))
                    throw new LuaJavaError("file '%s' does not exist".formatted(filePath));
                handle = createFileHandle(filePath, false, true, isPlusMode, false, false);
            }
            case 'w' -> {
                handle = createFileHandle(filePath, true, isPlusMode, true, false, true);
                handle.clear();
            }
            case 'a' -> {
                handle = createFileHandle(filePath, true, false, true, true, false);
            }
            default -> throw new IllegalStateException("unreachable");
        }
        return handle;
    }

    public LuaFsFileUD createFileHandle(String filePath, boolean autocreate, boolean handleCanRead, boolean handleCanWrite, boolean isAppendMode, boolean clearFileOnOpen) {
        if (openFilehandles.containsKey(new File(filePath)))
            throw new LuaJavaError("file is already opened");

        return new LuaFsFileUD(this, autocreate, filePath, handleCanRead, handleCanWrite, isAppendMode, clearFileOnOpen);
    }

    @LuaCallable
    public LuaObject list(String path) {
        // input can have slash or not at the end
        var realFsPath = getRealFsPath(normalizeEncodeAbsFolderPath(path));
        var depth = Path.of(path).getNameCount();
        //noinspection ConstantValue
        RuntimeAssert.RuntimeAssert(depth >= 0, "somehow depth was <0"); // allegedly always true, but we need to be absolutely sure
        if (!Files.isDirectory(realFsPath))
            throw new LuaJavaError("path '%s' does not exist or is not a directory".formatted(path));
        try (var objects = Files.list(realFsPath)) {
            return LuaObject.tableFromArray(objects.map(x -> {
                var luaPath = decodeFilename(x.subpath(getFsRealBasePath().getNameCount() + depth, x.getNameCount()).toString())
                        .replace("\\","/");
                return LuaObject.of(Files.isDirectory(x) ? luaPath + "/" : luaPath);
            }).toArray(LuaObject[]::new));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @LuaCallable
    public boolean fileExists(String path) {
        // no slash at the end
        if (isLuaPathDirectory(path))
            throw new LuaJavaError("Expected file path, but path ended with a slash.");

        var realPath = getRealFsPath(normalizeEncodeAbsFilePath(path));
        return Files.isRegularFile(realPath);
    }

    @LuaCallable
    public boolean directoryExists(String path) {
        // slash or no slash at th end
        var realPath = getRealFsPath(normalizeEncodeAbsFilePath(path));
        return Files.isDirectory(realPath);
    }

    @LuaCallable
    public void delete(String path) {
        // if slash at end, must be folder, otherwise can be file or folder
        var realPathInfo = getRealFileOrDirectoryPath(path, isLuaPathDirectory(path));
        var realPath = realPathInfo.realFsPath();
        RuntimeAssert.RuntimeAssert(realPath.startsWith(getFsRealBasePath()), "Somehow we tried to delete something outside of the filesystem. Please report this.");
        if (Files.isRegularFile(realPath)) {
            if (realPathInfo.isDirectory()) {
                throw new LuaJavaError("Path pointed at file, even though it ended with a slash and therefore a directory was expected.");
            }
            try {
                Files.delete(realPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (Files.isDirectory(realPath)) {
            var rootPath = getFsRealBasePath();
            try (Stream<Path> paths = Files.walk(realPath)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(x -> {
                    if (!x.toPath().equals(rootPath)) { // dont delete the actual fs root folder ever
                        if (x.isFile()) {
                            var handle = openFilehandles.get(x.toPath().subpath(rootPath.getNameCount(), x.toPath().getNameCount()).toFile());
                            if (handle != null)
                                handle.closeForDeletion();
                        }

                        //noinspection ResultOfMethodCallIgnored
                        x.delete();
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new LuaJavaError("File/directory '%s' does not exist".formatted(path));
        }
    }

    @LuaCallable
    public void move(String src, String dest) {
        // if slash at end, must be folder, otherwise can be file or folder
        var realSrcPath = getRealFsPath(normalizeEncodeAbsPath(src, true, false));
        var realDstPath = getRealFsPath(normalizeEncodeAbsPath(dest, true, false));
        RuntimeAssert.RuntimeAssert(realSrcPath.startsWith(getFsRealBasePath()), "Somehow we tried to delete something outside of the filesystem. Please report this. (src)");
        RuntimeAssert.RuntimeAssert(realDstPath.startsWith(getFsRealBasePath()), "Somehow we tried to delete something outside of the filesystem. Please report this. (dst)");

        if (Files.exists(realDstPath))
            throw new LuaJavaError("destination path already exists");

        if (!Files.exists(realSrcPath))
            throw new LuaJavaError("source path does not exist");

        var isDirectoryOperation = isLuaPathDirectory(src) || isLuaPathDirectory(dest);
        if (isDirectoryOperation && !Files.isDirectory(realSrcPath)) {
            throw new LuaJavaError("source path is not a directory (implied by trailing slash)");
        }

        try {
            Files.move(realSrcPath, realDstPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @LuaCallable
    public void makeDirectory(String path) {
        // input can have slash but need not
        var normalizedPath = normalizeEncodeAbsFolderPath(path);
        // traverse the chain and see if any parent folder name is already taken by a file, which would be illegal
        var currentFilePath = getFsRealBasePath();
        for (String segment : normalizedPath.split("/")) {
            currentFilePath = currentFilePath.resolve(segment);
            if (Files.isRegularFile(currentFilePath))
                throw new LuaJavaError("Unable to create directory or parents: a directory name is already in use by a file");
        }

        // actually create the folders
        try {
            Files.createDirectories(getRealFsPath(normalizedPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @LuaCallable
    public long spaceUsed() {
        return spaceUsed("/");
    }

    @LuaCallable
    public long spaceUsed(String path) {
        // if slash at end, must be folder, otherwise can be file or folder
        // TODO also make the user pay for file/folder attributes

        var realFsPath = getRealFileOrDirectoryPath(path, isLuaPathDirectory(path));
        var realPath = realFsPath.realFsPath();
        if (!realFsPath.isDirectory()) { // if its a single file
            return getFileCost(realPath) + getNameCost(realPath);
        }

        // otherwise traverse entire tree
        try (var paths = Files.walk(realPath, Integer.MAX_VALUE)) {
            long[] totalByteCount = new long[]{0};
            paths.forEach(pi -> {
                if (Files.isRegularFile(pi)) {
                    totalByteCount[0] += getFileCost(pi);
                }
                totalByteCount[0] += getNameCost(pi); // add an extra cost of 1 per file name letter
            });
            return totalByteCount[0];
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long getFileCost(Path realPath) {
        try {
            var openHandle = openFilehandles.get(realPath.subpath(getFsRealBasePath().getNameCount(), realPath.getNameCount()).toFile());
            return openHandle != null ? openHandle.getUnflushedSize() : Files.size(realPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private long getNameCost(Path realPath) {
        if (realPath.equals(getFsRealBasePath())) // no name-cost for the root folder of the filesystem
            return 0;

        return realPath.getName(realPath.getNameCount() - 1).toString().length();
    }

    @Override
    int getDiskId() {
        return diskStorageId;
    }

    // ########################### SERIALIZATION ###########################
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
        RuntimeAssert.RuntimeAssert(diskStorageId >= 0, "attempted to load a computer with disk id -1. Something must have gone wrong.");
        var storageFamilyName = reader.readString();
        var totalCapacityBytes = reader.readInt();

        return new ManagedMassStorageUD((LuaVirtualMachine) additionalData, isAccessible, storageFamilyName,
                totalCapacityBytes, diskStorageId);
    }

    // ############################## HELPERS ##############################
    private record PathSearchResult(boolean isDirectory, Path realFsPath) {
    }

    /**
     * Takes in a virtual path, and attempts to convert it to a directory or file path, depending on what exists.
     * If assertDirectory is set to true, throws if it is not a directory.
     *
     * @param virtualFsPath
     * @param assertDirectory
     * @return
     */
    private PathSearchResult getRealFileOrDirectoryPath(String virtualFsPath, boolean assertDirectory) {
        var realPath = getRealFsPath(normalizeEncodeAbsFilePath(virtualFsPath));
        if (Files.isDirectory(realPath))
            return new PathSearchResult(true, realPath);
        else if (Files.isRegularFile(realPath)) {
            if (assertDirectory)
                throw new LuaJavaError("Expected a directory path, found a file at the given path: '%s'".formatted(virtualFsPath));

            return new PathSearchResult(false, realPath);
        } else {
            throw new LuaJavaError("File/Directory '%s' does not exist".formatted(virtualFsPath));
        }
    }

    private static boolean isLuaPathDirectory(String path) {
        return path.endsWith("/") || path.endsWith("\\");
    }

    private static final String forbiddenChars = "<>:\"|?*";

    private static String normalizeEncodeAbsFilePath(String path) {
        return normalizeEncodeAbsPath(path, true, false);
    }

    private static String normalizeEncodeAbsFolderPath(String path) {
        return normalizeEncodeAbsPath(path, false, true);
    }

    private static String normalizeEncodeAbsPath(String path, boolean forceFile, boolean forceDirectory) {
        RuntimeAssert.RuntimeAssert(!forceFile || !forceDirectory, "somehow both forceFile and forceDirectory were true");
        path = encodeFilename(path);

        for (int i = 0; i < forbiddenChars.length(); i++) { // make sure no funny symbols are included like < or >
            if (path.indexOf(forbiddenChars.charAt(i)) != -1) {
                throw new LuaJavaError("File path contained illegal character '%s'".formatted(forbiddenChars.charAt(i)));
            }
        }

        for (int i = 0; i < path.length(); i++) { // make sure path chars are >=32 and <= 255
            var intValue = ((int) path.charAt(i));
            if (intValue < 32 || intValue > 255) {
                throw new LuaJavaError("File path contained illegal control character 0x%X".formatted(intValue));
            }
        }

        // normalize the path, i.e. process ./ ../ and empty segments
        var slashesNormalized = path.replace('\\', '/');
        boolean isFolder = (forceFile || forceDirectory) ?
                forceDirectory : // if we override the expected path type
                slashesNormalized.endsWith("/"); // otherwise autodetect it

        //noinspection ExtractMethodRecommender
        var normPath = new ArrayList<String>();
        for (var segment : slashesNormalized.split("/", -1)) {
            if (segment.equals(".") || segment.isEmpty()) {
                continue;
            } else if (segment.equals("..")) {
                if (normPath.isEmpty())
                    throw new LuaJavaError("path is invalid as it contained too many '..' segments");
                normPath.remove(normPath.size() - 1);
            } else {
                normPath.add(segment);
            }
        }
        return "/" + String.join("/", normPath) + (isFolder ? "/" : ""); // build the final, absolute virtual path
    }

    public Path getRealFsPath(String fsPath) {
        RuntimeAssert.RuntimeAssert(!fsPath.contains("../") && !fsPath.contains("..\\") && !fsPath.contains("./") &&
                                    !fsPath.contains(".\\"), "illegal characters in path?");

        while (fsPath.startsWith("/") || fsPath.startsWith("\\")) // trim leading slashes, just in case
            fsPath = fsPath.substring(1);

        return Path.of(getFsRealBasePath().toString(), fsPath); // this is safer than .resolve as it ignores leading slashes in the second argument
    }

    /**
     * Called when a new file handle has been opened
     *
     * @param path   the path of the referenced file. This is the in-fs path, so independent to the host filesystem path
     * @param handle
     */
    void onFileHandleOpened(String path, LuaFsFileUD handle) {
        RuntimeAssert.RuntimeAssert(!path.endsWith("/"), "unexpected path format");

        var existing = openFilehandles.put(new File(path), handle);
        RuntimeAssert.RuntimeAssert(existing == null, "somehow a handle was already open");
    }

    void onFileHandleClosed(String path, LuaFsFileUD handle) {
        RuntimeAssert.RuntimeAssert(!path.endsWith("/"), "unexpected path format");

        var existing = openFilehandles.remove(new File(path));
        RuntimeAssert.RuntimeAssert(existing != null, "somehow a handle was not tracked?");
        RuntimeAssert.RuntimeAssert(existing == handle, "somehow a different handle was associated with the path we just closed");
    }

    public static String encodeFilename(String filename) {
        if (!Config.escapeUppercaseCharactersOnHost) // no modification if not desired
            return filename;

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
        var decodedFilename = decodeFilenameOrNull(filename);
        RuntimeAssert.RuntimeAssert(decodedFilename != null, "filename decoding failed, restart the lua computer to fix invalid filenames, or encode them properly!");
        return decodedFilename;
    }

    public static String decodeFilenameOrNull(String filename) {
        if (!Config.escapeUppercaseCharactersOnHost) // no modification if not desired
            return filename;

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
    // #####################################################################
}
