package dev.asdf00.mc.advcomp.utils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class MiscUtil {
    // src: https://stackoverflow.com/questions/29076439/java-8-copy-directory-recursively/60621544#60621544
    public static void copyFolderRecursively(Path source, Path target, CopyOption... options) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file).toString()), options);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static String AcIpToString(int acIpAddress) {
        var rv = new StringBuilder();
        var hexRepr = String.format("%08X", acIpAddress);
        for (int i = 0; i < 4; i++) {
            if (i > 0)
                rv.append('.');
            rv.append(hexRepr, i * 2, i * 2 + 2);
        }
        return rv.toString();
    }
}
