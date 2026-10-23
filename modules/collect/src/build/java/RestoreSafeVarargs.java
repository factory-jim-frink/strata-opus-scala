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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Build-only tool restoring {@code @SafeVarargs} on the Java varargs methods of {@code Guavate}.
 * <p>
 * The Java implementation of {@code Guavate} annotated eight varargs methods with {@code @SafeVarargs}.
 * Scala 3 generates the Java varargs forwarders for {@code @varargs} methods but cannot place
 * {@code @SafeVarargs} on them, so Java callers would see unchecked generic array creation warnings.
 * This tool adds exactly one runtime-visible {@code java.lang.SafeVarargs} annotation to exactly those
 * eight methods, matched by name, JVM descriptor and access flags.
 * <p>
 * The tool fails closed: if any target is missing, has unexpected flags, is duplicated, or if any other
 * method already carries {@code SafeVarargs} or looks like an unexpected varargs overload of a target name,
 * it exits with status 1 and leaves the class file unchanged.
 * If every target is already annotated, the class file bytes are left untouched.
 * <p>
 * Usage: {@code java RestoreSafeVarargs <classes-directory>}
 */
public final class RestoreSafeVarargs {

  /** The internal name of the class to rewrite. */
  static final String CLASS_NAME = "com/opengamma/strata/collect/Guavate";
  /** The descriptor of the annotation. */
  static final String SAFE_VARARGS_DESC = "Ljava/lang/SafeVarargs;";
  /** The exact access flags each target must have. */
  static final int TARGET_ACCESS = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VARARGS;
  /** The targets, as name + descriptor, taken from the baseline Java bytecode. */
  static final List<String> TARGETS = Collections.unmodifiableList(Arrays.asList(
      "concatToList([Ljava/lang/Iterable;)Lcom/google/common/collect/ImmutableList;",
      "concatItemsToList(Ljava/lang/Iterable;[Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;",
      "concatToSet([Ljava/lang/Iterable;)Lcom/google/common/collect/ImmutableSet;",
      "combineMapsOverwriting(Ljava/util/Map;[Ljava/util/Map$Entry;)Lcom/google/common/collect/ImmutableMap;",
      "firstNonEmpty([Ljava/util/function/Supplier;)Ljava/util/Optional;",
      "firstNonEmpty([Ljava/util/Optional;)Ljava/util/Optional;",
      "list(Ljava/lang/Object;[Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;",
      "set(Ljava/lang/Object;[Ljava/lang/Object;)Lcom/google/common/collect/ImmutableSet;"));

  private RestoreSafeVarargs() {
  }

  /**
   * Runs the tool against {@code <classes-directory>/com/opengamma/strata/collect/Guavate.class}.
   *
   * @param args the classes directory
   */
  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("RestoreSafeVarargs: usage: RestoreSafeVarargs <classes-directory>");
      System.exit(2);
    }
    Path file = Paths.get(args[0]).resolve(CLASS_NAME + ".class");
    try {
      byte[] input = Files.readAllBytes(file);
      byte[] output = restore(input);
      if (Arrays.equals(input, output)) {
        System.out.println("RestoreSafeVarargs: all " + TARGETS.size() +
            " SafeVarargs annotations already present, class unchanged: " + file);
      } else {
        Files.write(file, output);
        System.out.println("RestoreSafeVarargs: restored SafeVarargs on " + countMissing(input) + " of " +
            TARGETS.size() + " methods (" + TARGETS.size() + " now annotated): " + file);
      }
    } catch (IOException | RuntimeException ex) {
      System.err.println("RestoreSafeVarargs: FAILED for " + file + ": " + ex.getMessage());
      System.exit(1);
    }
  }

  /**
   * Restores the annotations in the class bytes.
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
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if (!TARGETS.contains(name + descriptor)) {
          return mv;
        }
        return new MethodVisitor(Opcodes.ASM9, mv) {
          private boolean annotated;

          @Override
          public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (SAFE_VARARGS_DESC.equals(desc)) {
              annotated = true;
            }
            return super.visitAnnotation(desc, visible);
          }

          @Override
          public void visitCode() {
            addIfNeeded();
            super.visitCode();
          }

          @Override
          public void visitEnd() {
            addIfNeeded();
            super.visitEnd();
          }

          private void addIfNeeded() {
            if (!annotated) {
              annotated = true;
              super.visitAnnotation(SAFE_VARARGS_DESC, true).visitEnd();
            }
          }
        };
      }
    }, 0);
    byte[] output = writer.toByteArray();
    if (countMissing(output) != 0) {
      throw new IllegalStateException("internal error: annotations still missing after rewrite");
    }
    return output;
  }

  /**
   * Validates the class shape and returns the number of targets lacking the annotation.
   *
   * @param bytes the class file bytes
   * @return the number of targets lacking the annotation
   * @throws IllegalStateException if the class does not have the expected shape
   */
  static int countMissing(byte[] bytes) {
    ClassReader reader = new ClassReader(bytes);
    if (!CLASS_NAME.equals(reader.getClassName())) {
      throw new IllegalStateException("expected class " + CLASS_NAME + " but found " + reader.getClassName());
    }
    Map<String, int[]> found = new LinkedHashMap<>();
    Map<String, Integer> annotationCounts = new LinkedHashMap<>();
    List<String> problems = new ArrayList<>();
    Set<String> targetNames = new HashSet<>();
    for (String target : TARGETS) {
      targetNames.add(target.substring(0, target.indexOf('(')));
    }
    reader.accept(new ClassVisitor(Opcodes.ASM9) {
      @Override
      public MethodVisitor visitMethod(
          int access, String name, String descriptor, String signature, String[] exceptions) {
        String key = name + descriptor;
        boolean target = TARGETS.contains(key);
        if (target) {
          if (found.containsKey(key)) {
            problems.add("duplicate target method " + key);
          }
          found.put(key, new int[] {access});
          annotationCounts.put(key, 0);
        } else if (targetNames.contains(name) && (access & Opcodes.ACC_VARARGS) != 0) {
          problems.add("unexpected varargs candidate " + key);
        }
        return new MethodVisitor(Opcodes.ASM9) {
          @Override
          public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            if (SAFE_VARARGS_DESC.equals(desc)) {
              if (!target) {
                problems.add("SafeVarargs present on non-target method " + key);
              } else if (!visible) {
                problems.add("SafeVarargs present but not runtime-visible on " + key);
              } else {
                annotationCounts.put(key, annotationCounts.get(key) + 1);
              }
            }
            return null;
          }
        };
      }
    }, ClassReader.SKIP_CODE);
    int missing = 0;
    for (String target : TARGETS) {
      int[] access = found.get(target);
      if (access == null) {
        problems.add("missing target method " + target);
      } else {
        if (access[0] != TARGET_ACCESS) {
          problems.add("unexpected access flags 0x" + Integer.toHexString(access[0]) + " on " + target +
              ", expected 0x" + Integer.toHexString(TARGET_ACCESS));
        }
        int count = annotationCounts.get(target);
        if (count > 1) {
          problems.add("SafeVarargs present more than once on " + target);
        } else if (count == 0) {
          missing++;
        }
      }
    }
    if (!problems.isEmpty()) {
      throw new IllegalStateException("unexpected shape of " + CLASS_NAME + ": " + String.join("; ", problems));
    }
    return missing;
  }

}
