package dev.asdf00.mc.advcomp.items;

import dev.asdf00.jluavm.api.userdata.LuaCallable;
import dev.asdf00.jluavm.api.userdata.LuaDeserializer;
import dev.asdf00.jluavm.api.userdata.LuaUserData;
import dev.asdf00.jluavm.exceptions.LuaJavaError;
import dev.asdf00.jluavm.runtime.types.LuaObject;
import dev.asdf00.jluavm.utils.ByteArrayBuilder;
import dev.asdf00.jluavm.utils.ByteArrayReader;
import dev.asdf00.mc.advcomp.utils.RuntimeAssert;
import dev.asdf00.mc.advcomp.utils.list.internal.CharacterList;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class LuaFsFileUD implements LuaUserData {
    private ManagedMassStorageUD parentFilesystemUD;
    private final boolean canRead;
    private final boolean canWrite;
    private final boolean isAppend;
    private final String fsFilePath;
    private int ptr;

    private final boolean autocreate;
    private LuaObject luaIdentity;
    private CharacterList contents;

    public LuaFsFileUD(ManagedMassStorageUD parentFilesystemUD, boolean autocreate, String fsFilePath, boolean canRead, boolean canWrite, boolean isAppend, boolean clearFileOnOpen) {
        RuntimeAssert.RuntimeAssert(canRead || canWrite, "somehow the handle was nonread and nonwrite");
        RuntimeAssert.RuntimeAssert(!isAppend || canWrite, "somehow append was nonwrite");
        RuntimeAssert.RuntimeAssert(!isAppend || !canRead, "somehow append was readable");

        this.autocreate = autocreate;
        this.parentFilesystemUD = parentFilesystemUD;
        this.fsFilePath = fsFilePath;
        this.canRead = canRead;
        this.canWrite = canWrite;
        this.isAppend = isAppend;

        if (parentFilesystemUD != null) // if null, then we are loading --> defer the init
            finishInit(clearFileOnOpen);
    }

    private void finishInit(boolean clearFileOnOpen) {
        var realDiskFilePath = parentFilesystemUD.getRealFsPath(fsFilePath);
        var exists = Files.isRegularFile(realDiskFilePath);

        if (!exists) {
            if (autocreate) { // simply act as if we had read the contents
                contents = new CharacterList();
            } else {
                throw new IllegalStateException("somehow a file in the filesystem was missing even though it should be there");
            }
        } else {
            try {
                contents = new CharacterList();
                if (!clearFileOnOpen)
                    contents.addAllChars(Files.readString(realDiskFilePath).toCharArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        ptr = isAppend ? contents.size() : 0; // in the deserialization-case, the ptr value will be overwritten later
        parentFilesystemUD.onFileHandleOpened(fsFilePath, this);

        // if we concluded the filehandle is good, create the actual file on disk so that it shows up in list()
        if (!exists) {
            try {
                Files.createFile(realDiskFilePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @LuaCallable
    public LuaObject read(int count) {
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        if (!canRead) throw new LuaJavaError("filehandle is writeonly!");
        if (count == -1)
            count = Integer.MAX_VALUE;
        else if (count < -1) {
            throw new LuaJavaError("read length %d is invalid. must be >= -1".formatted(count));
        }

        if (ptr >= contents.size())
            return LuaObject.NIL;

        var sb = new StringBuilder(Math.min(contents.size() - ptr, count));
        for (long i = ptr; i < Math.min(contents.size(), (long) ptr + (long) count); i++) { // needs to be long so that ptr+int32.maxVal doesnt overflow
            sb.append(contents.get((int) i));
            ptr++;
        }

        return LuaObject.of(sb.toString());
    }

    @LuaCallable
    public void write(String s) { // make sure this operation fails if we dont have enough space
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        if (!canWrite) throw new LuaJavaError("filehandle is readonly!");

        for (int i = 0; i < s.length(); i++) {
            if (i < contents.size()) // index already exists -> overwrite
                contents.set(ptr, s.charAt(i));
            else // or just add it
                contents.add(s.charAt(i));

            ptr++;
        }
    }

    @LuaCallable
    public void seek(String relativeTo, int offset) {
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        if (isAppend) throw new LuaJavaError("filehandle can only append!");

        int newPtr;
        switch (relativeTo) {
            case "start" -> newPtr = offset;
            case "end" -> newPtr = contents.size() - offset;
            case "relative" -> newPtr = ptr + offset;
            default -> throw new LuaJavaError("invalid seek mode '%s' given (argument #1).".formatted(relativeTo));
        }

        if (newPtr < 0 || newPtr > contents.size())
            throw new LuaJavaError("resulting seek position is out of bounds (%d)!".formatted(newPtr));

        ptr = newPtr;
    }

    /**
     * truncates the file to a length of zero
     */
    public void clear() {
        if (contents == null) throw new IllegalStateException("filehandle is already closed");
        contents.clear();
    }

    @LuaCallable
    public void flush() {
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        if (!canWrite)
            throw new LuaJavaError("cannot call flush on a readonly handle");

        try {
            Files.writeString(parentFilesystemUD.getRealFsPath(fsFilePath), new String(contents.toCharArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @LuaCallable
    public void close() {
        if (contents == null) throw new LuaJavaError("filehandle is already closed");
        parentFilesystemUD.onFileHandleClosed(fsFilePath, this);
        if (canWrite)
            flush();
        contents = null;
    }

    public void closeForDeletion() { // a simpler version of 'close' which doesnt even save; this is for deletion only
        RuntimeAssert.RuntimeAssert(contents != null, "somehow handle was already closed?");
        contents = null;
        parentFilesystemUD.onFileHandleClosed(fsFilePath, this);
    }

    @Override
    public byte[] luaSerialize(List<byte[]> serialData, Map<LuaObject, Integer> mappedObjs, Object additionalData) {
        return new ByteArrayBuilder()
                .append(LuaObject.of(parentFilesystemUD).serialize(serialData, mappedObjs, additionalData))
                .append(fsFilePath)
                .append(canRead)
                .append(canWrite)
                .append(isAppend)
                .append(ptr)
                .toArray();
    }

    @LuaDeserializer
    public static LuaFsFileUD luaDeserialize(LuaObject[] objs, ByteArrayReader reader, Queue<Runnable> postActions, Object additionalData) {
        var parentFs = objs[reader.readInt()];
        var filePath = reader.readString();
        var bRead = reader.readBool();
        var bWrite = reader.readBool();
        var bAppend = reader.readBool();
        var ptr = reader.readInt();


        var rv = new LuaFsFileUD(null, false, filePath, bRead, bWrite, bAppend, false); // dont autocreate as the file should exist already at this point
        postActions.add(() -> {
            rv.parentFilesystemUD = ((ManagedMassStorageUD) parentFs.refVal);
            rv.finishInit(false);
            rv.ptr = ptr; // set ptr afterwards as it gets modified by the init function
        });
        return rv;
    }

    @Override
    public LuaObject getSelfAsLuaObject() {
        return luaIdentity;
    }

    @Override
    public void setSelfAsLuaObject(LuaObject self) {
        this.luaIdentity = self;
    }

    public long getUnflushedSize() {
        return fsFilePath.length();
    }
}