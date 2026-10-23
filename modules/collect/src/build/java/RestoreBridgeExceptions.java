/*
 * Copyright (C) 2026 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Build-only tool restoring {@code throws IOException} on the {@code InputStream} bridges of {@code ArrayByteSource}.
 * <p>
 * {@code ArrayByteSource} overrides Guava's {@code ByteSource.openStream()} and {@code openBufferedStream()},
 * both declared {@code throws IOException}, with covariant methods returning {@code ByteArrayInputStream}
 * that throw no checked exception. javac copies {@code throws IOException} onto the two
 * {@code InputStream}-returning bridge methods; Scala 3 bridges carry no exceptions. This tool adds the
 * {@code Exceptions} attribute {@code java/io/IOException} to exactly those two bridges.
 * The concrete {@code ByteArrayInputStream} methods must keep declaring no exceptions.
 * <p>
 * The tool fails closed: if a bridge or concrete method is missing, has unexpected flags or exceptions,
 * or another {@code openStream}/{@code openBufferedStream} overload appears, it exits with status 1 and
 * leaves the class file unchanged. If both bridges already declare exactly {@code IOException}
 * (for example a javac-compiled class), the class file bytes are left untouched.
 * <p>
 * Usage: {@code java RestoreBridgeExceptions <classes-directory>}
 */
public final class RestoreBridgeExceptions {

  /** The internal name of the class to rewrite. */
  static final String CLASS_NAME = "com/opengamma/strata/collect/io/ArrayByteSource";
  /** The exception to restore. */
  static final String IO_EXCEPTION = "java/io/IOException";
  /** The exact access flags each bridge must have. */
  static final int BRIDGE_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC;
  /** The bridge targets, as name + descriptor. */
  static final List<String> BRIDGES = Collections.unmodifiableList(Arrays.asList(
      "openStream()Ljava/io/InputStream;",
      "openBufferedStream()Ljava/io/InputStream;"));
  /** The concrete methods the bridges delegate to, which must not declare exceptions. */
  static final List<String> CONCRETES = Collections.unmodifiableList(Arrays.asList(
      "openStream()Ljava/io/ByteArrayInputStream;",
      "openBufferedStream()Ljava/io/ByteArrayInputStream;"));

  private RestoreBridgeExceptions() {
  }

  /**
   * Runs the tool against {@code <classes-directory>/com/opengamma/strata/collect/io/ArrayByteSource.class}.
   *
   * @param args the classes directory
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("RestoreBridgeExceptions: usage: RestoreBridgeExceptions <classes-directory>");
      System.exit(2);
    }
    Path file = Paths.get(args[0]).resolve(CLASS_NAME + ".class");
    try {
      byte[] input = Files.readAllBytes(file);
      int missing = countMissing(input);
      byte[] output = restore(input);
      if (Arrays.equals(input, output)) {
        System.out.println("RestoreBridgeExceptions: both bridges already declare IOException, class unchanged: " + file);
      } else {
        Files.write(file, output);
        System.out.println("RestoreBridgeExceptions: restored IOException on " + missing + " of " + BRIDGES.size() +
            " bridges (" + BRIDGES.size() + " now declare it): " + file);
      }
    } catch (IOException | RuntimeException ex) {
      System.err.println("RestoreBridgeExceptions: FAILED for " + file + ": " + ex.getMessage());
      System.exit(1);
    }
  }

  /**
   * Restores the bridge exceptions in the class bytes.
   * <p>
   * The input array is returned as-is when nothing needs to change.
   *
   * @param input the class file bytes
   * @return the restored class file bytes
   * @throws IllegalStateException if the class does not have the expected shape
   */
  public static byte[] restore(byte[] input) {
    if (countMissing(input) == 0) {
      return input;
    }
    ClassReader reader = new ClassReader(input);
    // passing the reader lets ASM copy the constant pool and every untouched method verbatim
    ClassWriter writer = new ClassWriter(reader, 0);
    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public MethodVisitor visitMethod(
          int access, String name, String descriptor, String signature, String[] exceptions) {
        String[] newExceptions = exceptions;
        if (BRIDGES.contains(name + descriptor) && (exceptions == null || exceptions.length == 0)) {
          newExceptions = new String[] {IO_EXCEPTION};
        }
        return super.visitMethod(access, name, descriptor, signature, newExceptions);
      }
    }, 0);
    byte[] output = writer.toByteArray();
    if (countMissing(output) != 0) {
      throw new IllegalStateException("internal error: exceptions still missing after rewrite");
    }
    return output;
  }

  /**
   * Validates the class shape and returns the number of bridges lacking the exception.
   *
   * @param bytes the class file bytes
   * @return the number of bridges lacking the exception
   * @throws IllegalStateException if the class does not have the expected shape
   */
  static int countMissing(byte[] bytes) {
    ClassReader reader = new ClassReader(bytes);
    if (!CLASS_NAME.equals(reader.getClassName())) {
      throw new IllegalStateException("expected class " + CLASS_NAME + " but found " + reader.getClassName());
    }
    Map<String, Integer> access = new LinkedHashMap<>();
    Map<String, List<String>> declared = new LinkedHashMap<>();
    List<String> problems = new ArrayList<>();
    reader.accept(new ClassVisitor(Opcodes.ASM9) {
      @Override
      public MethodVisitor visitMethod(
          int acc, String name, String descriptor, String signature, String[] exceptions) {
        String key = name + descriptor;
        if (BRIDGES.contains(key) || CONCRETES.contains(key)) {
          if (access.containsKey(key)) {
            problems.add("duplicate method " + key);
          }
          access.put(key, acc);
          declared.put(key, exceptions == null ? Collections.<String>emptyList() : Arrays.asList(exceptions));
        } else if (name.equals("openStream") || name.equals("openBufferedStream")) {
          problems.add("unexpected candidate method " + key);
        }
        return null;
      }
    }, ClassReader.SKIP_CODE);
    int missing = 0;
    for (String bridge : BRIDGES) {
      Integer acc = access.get(bridge);
      if (acc == null) {
        problems.add("missing bridge method " + bridge);
        continue;
      }
      if (acc != BRIDGE_ACCESS) {
        problems.add("unexpected access flags 0x" + Integer.toHexString(acc) + " on bridge " + bridge +
            ", expected 0x" + Integer.toHexString(BRIDGE_ACCESS));
      }
      List<String> exceptions = declared.get(bridge);
      if (exceptions.isEmpty()) {
        missing++;
      } else if (!exceptions.equals(Collections.singletonList(IO_EXCEPTION))) {
        problems.add("unexpected exceptions " + exceptions + " on bridge " + bridge);
      }
    }
    for (String concrete : CONCRETES) {
      Integer acc = access.get(concrete);
      if (acc == null) {
        problems.add("missing concrete method " + concrete);
        continue;
      }
      int forbidden = Opcodes.ACC_STATIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_ABSTRACT;
      if ((acc & Opcodes.ACC_PUBLIC) == 0 || (acc & forbidden) != 0) {
        problems.add("unexpected access flags 0x" + Integer.toHexString(acc) + " on concrete " + concrete);
      }
      if (!declared.get(concrete).isEmpty()) {
        problems.add("concrete method " + concrete + " declares exceptions " + declared.get(concrete));
      }
    }
    if (!problems.isEmpty()) {
      throw new IllegalStateException("unexpected shape of " + CLASS_NAME + ": " + String.join("; ", problems));
    }
    return missing;
  }

}
