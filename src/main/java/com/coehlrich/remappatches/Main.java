package com.coehlrich.remappatches;

import net.minecraftforge.srgutils.IMappingFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        try {
            Pattern pattern = Pattern.compile("(m|f|p)_\\d+_");
            File original = new File(args[0]);
            File newMappingsFile = new File(args[1]);
            Path remapPath = Path.of(args[2]);
            IMappingFile originalMappings = IMappingFile.load(original);
            IMappingFile newMappings = IMappingFile.load(newMappingsFile);

            IMappingFile result = originalMappings.reverse().chain(newMappings);
            Map<String, String> originalToNew = new HashMap<>();
            originalToNew.putAll(result.getClasses().stream()
                    .map(IMappingFile.IClass::getMethods)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(IMappingFile.IMethod::getOriginal, IMappingFile.IMethod::getMapped, (first, second) -> {
                        if (first.equals(second)) {
                            return first;
                        } else {
                            throw new RuntimeException(first + " != " + second);
                        }
                    })));

            originalToNew.putAll(result.getClasses().stream()
                    .map(IMappingFile.IClass::getFields)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(IMappingFile.IField::getOriginal, IMappingFile.IField::getMapped, (first, second) -> {
                        if (first.equals(second)) {
                            return first;
                        } else {
                            throw new RuntimeException(first + " != " + second);
                        }
                    })));

            originalToNew.putAll(result.getClasses().stream()
                    .map(IMappingFile.IClass::getMethods)
                    .flatMap(Collection::stream)
                    .map(IMappingFile.IMethod::getParameters)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(IMappingFile.IParameter::getOriginal, IMappingFile.IParameter::getMapped, (first, second) -> {
                        if (first.equals(second)) {
                            return first;
                        } else {
                            throw new RuntimeException(first + " != " + second);
                        }
                    })));
            Files.walkFileTree(remapPath, new SimpleFileVisitor<>() {
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    System.out.println("Visiting " + dir);
                    return FileVisitResult.CONTINUE;
                }

                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String content = Files.readString(file);
                    String result = pattern.matcher(content).replaceAll((match) -> {
                        String replacement = originalToNew.get(match.group());
                        return replacement != null ? replacement : match.group();
                    });
                    Files.writeString(file, result);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

}
