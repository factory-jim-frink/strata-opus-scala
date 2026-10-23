/*
 * Copyright (C) 2026 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import java.util.stream.Stream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Standalone controls for {@link RestoreSafeVarargs}.
 * <p>
 * Fixtures are generated with ASM; the real Scala-compiled {@code Guavate} in the classes directory
 * (already processed at {@code process-classes}) is also checked.
 * <p>
 * Usage: {@code java RestoreSafeVarargsControls <classes-directory>}
 */
public final class RestoreSafeVarargsControls {

  private static final String SEQ = "Lscala/collection/immutable/Seq;";
  private static final String DEPRECATED_DESC = "Ljava/lang/Deprecated;";
  /** Methods that must never be annotated: Scala Seq overloads and unrelated methods. */
  private static final List<String[]> NON_TARGETS = Arrays.asList(
      new String[] {"9", "concatToList", "(" + SEQ + ")Lcom/google/common/collect/ImmutableList;"},
      new String[] {"9", "concatItemsToList", "(Ljava/lang/Iterable;" + SEQ + ")Lcom/google/common/collect/ImmutableList;"},
      new String[] {"9", "concatToSet", "(" + SEQ + ")Lcom/google/common/collect/ImmutableSet;"},
      new String[] {"9", "combineMapsOverwriting", "(Ljava/util/Map;" + SEQ + ")Lcom/google/common/collect/ImmutableMap;"},
      new String[] {"9", "combineMapsOverwriting", "(Ljava/util/Map;Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"},
      new String[] {"9", "firstNonEmpty", "(" + SEQ + ")Ljava/util/Optional;"},
      new String[] {"9", "list", "(Ljava/lang/Object;" + SEQ + ")Lcom/google/common/collect/ImmutableList;"},
      new String[] {"9", "set", "(Ljava/lang/Object;" + SEQ + ")Lcom/google/common/collect/ImmutableSet;"},
      new String[] {"137", "ensureOnlyOne", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/util/function/BinaryOperator;"},
      new String[] {"9", "deprecatedHelper", "()Ljava/lang/Object;", "deprecated"});

  private static final List<String> RESULTS = new ArrayList<>();
  private static int failures;

  private RestoreSafeVarargsControls() {
  }

  /**
   * Runs the controls.
   *
   * @param args the classes directory holding the processed real {@code Guavate}
   * @throws Exception if an unexpected error occurs
   */
  public static void main(String[] args) throws Exception {
    Path classes = Paths.get(args[0]);
    byte[] real = Files.readAllBytes(classes.resolve(RestoreSafeVarargs.CLASS_NAME + ".class"));
    Set<String> none = Collections.emptySet();
    Set<String> all = new HashSet<>(RestoreSafeVarargs.TARGETS);
    Set<String> partial = new HashSet<>(RestoreSafeVarargs.TARGETS.subList(0, 3));

    run("1 unannotated input is restored", () -> {
      byte[] out = RestoreSafeVarargs.restore(fixture(none, null, -1, null));
      checkExactlyTargetsAnnotated(out);
    });
    run("2 partially annotated input is completed", () -> {
      byte[] in = fixture(partial, null, -1, null);
      check(RestoreSafeVarargs.countMissing(in) == 5, "fixture should lack 5 annotations");
      checkExactlyTargetsAnnotated(RestoreSafeVarargs.restore(in));
    });
    run("3 already annotated input is byte-for-byte unchanged", () -> {
      byte[] in = fixture(all, null, -1, null);
      byte[] out = RestoreSafeVarargs.restore(in);
      check(in == out && Arrays.equals(in, out), "fixture bytes changed");
      byte[] realCopy = real.clone();
      check(Arrays.equals(real, RestoreSafeVarargs.restore(realCopy)), "real Guavate bytes changed");
      checkExactlyTargetsAnnotated(real);
    });
    run("4 restoring twice equals restoring once", () -> {
      for (Set<String> start : Arrays.asList(none, partial)) {
        byte[] once = RestoreSafeVarargs.restore(fixture(start, null, -1, null));
        byte[] twice = RestoreSafeVarargs.restore(once);
        check(Arrays.equals(once, twice), "second restore changed bytes");
      }
      byte[] once = RestoreSafeVarargs.restore(strip(real));
      check(Arrays.equals(once, RestoreSafeVarargs.restore(once)), "second restore of real Guavate changed bytes");
    });
    run("5 Seq overloads and unrelated methods are untouched", () -> {
      byte[] in = fixture(partial, null, -1, null);
      byte[] out = RestoreSafeVarargs.restore(in);
      Map<String, List<String>> before = annotations(in);
      Map<String, List<String>> after = annotations(out);
      for (String[] nonTarget : NON_TARGETS) {
        String key = nonTarget[1] + nonTarget[2];
        check(after.containsKey(key), "missing " + key);
        check(before.get(key).equals(after.get(key)), "annotations changed on " + key);
        check(!after.get(key).contains(RestoreSafeVarargs.SAFE_VARARGS_DESC + ":true"), "annotated " + key);
      }
      check(after.get("deprecatedHelper()Ljava/lang/Object;").equals(
          Collections.singletonList(DEPRECATED_DESC + ":true")), "unrelated annotation lost");
      int seqOverloads = 0;
      for (Map.Entry<String, List<String>> entry : annotations(real).entrySet()) {
        if (entry.getKey().contains(SEQ)) {
          seqOverloads++;
          check(!entry.getValue().contains(RestoreSafeVarargs.SAFE_VARARGS_DESC + ":true"),
              "real Seq overload annotated: " + entry.getKey());
        }
      }
      check(seqOverloads >= 8, "real Guavate should have at least 8 Seq overloads, found " + seqOverloads);
    });
    run("6 non-annotation bytecode is identical after normalisation", () -> {
      for (Set<String> start : Arrays.asList(none, partial)) {
        byte[] in = fixture(start, null, -1, null);
        check(Arrays.equals(normalise(in, true), normalise(RestoreSafeVarargs.restore(in), true)),
            "fixture bytecode differs beyond SafeVarargs");
      }
      byte[] stripped = strip(real);
      check(RestoreSafeVarargs.countMissing(stripped) == 8, "stripped real Guavate should lack 8 annotations");
      byte[] restored = RestoreSafeVarargs.restore(stripped);
      check(Arrays.equals(normalise(stripped, true), normalise(restored, true)),
          "real Guavate bytecode differs beyond SafeVarargs");
      check(Arrays.equals(normalise(real, false), normalise(restored, false)),
          "re-restored real Guavate differs from the processed class");
    });
    run("7 missing targets and unexpected shapes fail closed", () -> {
      String target = RestoreSafeVarargs.TARGETS.get(6);
      expectFailure("missing target", fixture(none, target, -1, null));
      expectFailure("target without ACC_VARARGS", fixture(none, null, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, null));
      expectFailure("non-static target", fixture(none, null, Opcodes.ACC_PUBLIC | Opcodes.ACC_VARARGS, null));
      expectFailure("unexpected varargs candidate",
          fixture(none, null, -1, new String[] {"137", "list", "([Ljava/lang/Object;)Ljava/lang/Object;"}));
      expectFailure("SafeVarargs on unrelated method",
          fixture(none, null, -1, new String[] {"137", "other", "([Ljava/lang/Object;)Ljava/lang/Object;", "safe"}));
      expectFailure("SafeVarargs on Seq-like overload",
          fixture(none, null, -1, new String[] {"9", "set", "(" + SEQ + "I)Ljava/lang/Object;", "safe"}));
      expectFailure("duplicate annotation", annotateTwice(RestoreSafeVarargs.restore(fixture(none, null, -1, null))));
      expectFailure("wrong class", rename(fixture(none, null, -1, null), "com/opengamma/strata/collect/Other"));
      Path dir = Files.createTempDirectory("restore-safevarargs-control");
      Path file = dir.resolve(RestoreSafeVarargs.CLASS_NAME + ".class");
      Files.createDirectories(file.getParent());
      byte[] bad = fixture(none, target, -1, null);
      Files.write(file, bad);
      int exit = runMain("RestoreSafeVarargs", dir);
      check(exit == 1, "main should exit 1 on bad shape but exited " + exit);
      check(Arrays.equals(bad, Files.readAllBytes(file)), "main modified the class file despite failing");
      deleteTree(dir);
    });

    for (String result : RESULTS) {
      System.out.println("RestoreSafeVarargsControls: " + result);
    }
    System.out.println("RestoreSafeVarargsControls: " + (RESULTS.size() - failures) + " of " + RESULTS.size() +
        " controls passed");
    if (failures > 0 || RESULTS.size() != 7) {
      System.exit(1);
    }
  }

  //-------------------------------------------------------------------------
  private interface Control {
    void run() throws Exception;
  }

  private static void run(String name, Control control) {
    try {
      control.run();
      RESULTS.add("control " + name + ": PASS");
    } catch (Exception | Error ex) {
      failures++;
      RESULTS.add("control " + name + ": FAIL: " + ex);
    }
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  private static void expectFailure(String what, byte[] bytes) {
    try {
      RestoreSafeVarargs.restore(bytes);
    } catch (IllegalStateException ex) {
      return;
    }
    throw new AssertionError("expected failure for " + what);
  }

  // runs the tool's main in a separate JVM, as it calls System.exit, and deletes the temporary directory
  private static int runMain(String mainClass, Path dir) throws Exception {
    String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    ProcessBuilder pb = new ProcessBuilder(
        java, "-cp", System.getProperty("java.class.path"), mainClass, dir.toString());
    pb.redirectErrorStream(true);
    Process process = pb.start();
    String output = new String(readAll(process.getInputStream()), StandardCharsets.UTF_8);
    int exit = process.waitFor();
    check(output.contains(mainClass + ": FAILED"), "main did not report the failure: " + output);
    return exit;
  }

  private static byte[] readAll(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    for (int n = in.read(buffer); n >= 0; n = in.read(buffer)) {
      out.write(buffer, 0, n);
    }
    return out.toByteArray();
  }

  private static void deleteTree(Path dir) throws IOException {
    List<Path> paths = new ArrayList<>();
    try (Stream<Path> stream = Files.walk(dir)) {
      stream.forEach(paths::add);
    }
    Collections.reverse(paths);
    for (Path path : paths) {
      Files.delete(path);
    }
  }

  private static void checkExactlyTargetsAnnotated(byte[] bytes) {
    Map<String, List<String>> annotations = annotations(bytes);
    int total = 0;
    for (Map.Entry<String, List<String>> entry : annotations.entrySet()) {
      int count = Collections.frequency(entry.getValue(), RestoreSafeVarargs.SAFE_VARARGS_DESC + ":true");
      boolean target = RestoreSafeVarargs.TARGETS.contains(entry.getKey());
      check(count == (target ? 1 : 0), "unexpected SafeVarargs count " + count + " on " + entry.getKey());
      check(!entry.getValue().contains(RestoreSafeVarargs.SAFE_VARARGS_DESC + ":false"),
          "invisible SafeVarargs on " + entry.getKey());
      total += count;
    }
    check(total == 8, "expected 8 SafeVarargs annotations, found " + total);
    check(RestoreSafeVarargs.countMissing(bytes) == 0, "restored class still lacks annotations");
  }

  //-------------------------------------------------------------------------
  // name+descriptor -> annotation descriptors with visibility
  private static Map<String, List<String>> annotations(byte[] bytes) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        List<String> list = new ArrayList<>();
        result.put(name + desc, list);
        return new MethodVisitor(Opcodes.ASM9) {
          @Override
          public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
            list.add(annotation + ":" + visible);
            return null;
          }
        };
      }
    }, ClassReader.SKIP_CODE);
    return result;
  }

  // rebuilds the class from scratch, optionally dropping SafeVarargs, so constant pool layout cannot hide differences
  private static byte[] normalise(byte[] bytes, boolean dropSafeVarargs) {
    ClassWriter writer = new ClassWriter(0);
    new ClassReader(bytes).accept(dropSafeVarargs ? new SafeVarargsStripper(writer) : writer, 0);
    return writer.toByteArray();
  }

  private static byte[] strip(byte[] bytes) {
    ClassReader reader = new ClassReader(bytes);
    ClassWriter writer = new ClassWriter(reader, 0);
    reader.accept(new SafeVarargsStripper(writer), 0);
    return writer.toByteArray();
  }

  // no ClassReader is passed to the writer, as ASM would then copy the method's attributes verbatim
  private static byte[] annotateTwice(byte[] bytes) {
    ClassWriter writer = new ClassWriter(0);
    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (RestoreSafeVarargs.TARGETS.get(0).equals(name + desc)) {
          mv.visitAnnotation(RestoreSafeVarargs.SAFE_VARARGS_DESC, true).visitEnd();
        }
        return mv;
      }
    }, 0);
    return writer.toByteArray();
  }

  private static byte[] rename(byte[] bytes, String newName) {
    ClassReader reader = new ClassReader(bytes);
    ClassWriter writer = new ClassWriter(0);
    reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, newName, signature, superName, interfaces);
      }
    }, 0);
    return writer.toByteArray();
  }

  private static final class SafeVarargsStripper extends ClassVisitor {
    SafeVarargsStripper(ClassVisitor next) {
      super(Opcodes.ASM9, next);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, desc, signature, exceptions)) {
        @Override
        public AnnotationVisitor visitAnnotation(String annotation, boolean visible) {
          return RestoreSafeVarargs.SAFE_VARARGS_DESC.equals(annotation) ? null : super.visitAnnotation(annotation, visible);
        }
      };
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Generates a Guavate-shaped fixture.
   *
   * @param annotated  the targets to pre-annotate
   * @param omit  a target to omit, or null
   * @param targetAccess  access flags for every target, or -1 for the expected flags
   * @param extra  an extra method as {access, name, descriptor, optional "safe"/"deprecated"}, or null
   * @return the class bytes
   */
  private static byte[] fixture(Set<String> annotated, String omit, int targetAccess, String[] extra) {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
        RestoreSafeVarargs.CLASS_NAME, null, "java/lang/Object", null);
    Map<String, String[]> methods = new LinkedHashMap<>();
    int access = targetAccess == -1 ? RestoreSafeVarargs.TARGET_ACCESS : targetAccess;
    for (String target : RestoreSafeVarargs.TARGETS) {
      if (!target.equals(omit)) {
        int paren = target.indexOf('(');
        methods.put(target, new String[] {Integer.toString(access), target.substring(0, paren),
            target.substring(paren), annotated.contains(target) ? "safe" : ""});
      }
    }
    for (String[] nonTarget : NON_TARGETS) {
      methods.put(nonTarget[1] + nonTarget[2], nonTarget);
    }
    if (extra != null) {
      methods.put(extra[1] + extra[2], extra);
    }
    for (String[] method : methods.values()) {
      MethodVisitor mv = cw.visitMethod(Integer.parseInt(method[0]), method[1], method[2], null, null);
      String marker = method.length > 3 ? method[3] : "";
      if (marker.equals("deprecated")) {
        mv.visitAnnotation(DEPRECATED_DESC, true).visitEnd();
      }
      if (marker.equals("safe")) {
        mv.visitAnnotation(RestoreSafeVarargs.SAFE_VARARGS_DESC, true).visitEnd();
      }
      mv.visitCode();
      check(Type.getReturnType(method[2]).getSort() == Type.OBJECT, "fixture methods return objects");
      mv.visitInsn(Opcodes.ACONST_NULL);
      mv.visitInsn(Opcodes.ARETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
    cw.visitEnd();
    return cw.toByteArray();
  }

}
