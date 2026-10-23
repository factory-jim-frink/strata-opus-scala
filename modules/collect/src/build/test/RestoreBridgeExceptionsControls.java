/*
 * Copyright (C) 2026 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Standalone controls for {@link RestoreBridgeExceptions}.
 * <p>
 * Fixtures are generated with ASM, plus a real Scala 3 compiled {@code ArrayByteSource}-shaped class
 * (covariant {@code ByteArrayInputStream} overrides of Guava's {@code ByteSource} methods).
 * The real {@code ArrayByteSource} in the classes directory (already processed at {@code process-classes})
 * is also checked.
 * <p>
 * Usage: {@code java RestoreBridgeExceptionsControls <classes-directory> <scala-fixture-classes-directory>}
 */
public final class RestoreBridgeExceptionsControls {

  private static final String FILE = RestoreBridgeExceptions.CLASS_NAME + ".class";
  private static final List<String> IOE = Collections.singletonList(RestoreBridgeExceptions.IO_EXCEPTION);
  private static final List<String> NONE = Collections.emptyList();
  private static final int BRIDGE = RestoreBridgeExceptions.BRIDGE_ACCESS;
  private static final String OPEN = RestoreBridgeExceptions.BRIDGES.get(0);
  private static final String OPEN_BUFFERED = RestoreBridgeExceptions.BRIDGES.get(1);

  private static final List<String> RESULTS = new ArrayList<>();
  private static int failures;

  private RestoreBridgeExceptionsControls() {
  }

  /**
   * Runs the controls.
   *
   * @param args the classes directory and the Scala fixture classes directory
   * @throws Exception if an unexpected error occurs
   */
  public static void main(String[] args) throws Exception {
    byte[] real = Files.readAllBytes(Paths.get(args[0]).resolve(FILE));
    byte[] scala = Files.readAllBytes(Paths.get(args[1]).resolve(FILE));

    run("1 bridges without exceptions are restored (ASM and Scala 3 fixtures)", () -> {
      byte[] out = RestoreBridgeExceptions.restore(fixture(spec()));
      checkRestored(out);
      check(RestoreBridgeExceptions.countMissing(scala) == 2, "Scala fixture bridges should lack IOException");
      byte[] scalaOut = RestoreBridgeExceptions.restore(scala);
      checkRestored(scalaOut);
      checkLoadsAndRuns(scalaOut);
    });
    run("2 one bridge already restored is completed", () -> {
      Map<String, Object[]> spec = spec();
      spec.put(OPEN, new Object[] {BRIDGE, IOE});
      byte[] in = fixture(spec);
      check(RestoreBridgeExceptions.countMissing(in) == 1, "fixture should lack 1 exception");
      checkRestored(RestoreBridgeExceptions.restore(in));
      byte[] scalaPartial = RestoreBridgeExceptions.restore(scala);
      scalaPartial = setExceptions(scalaPartial, OPEN_BUFFERED, null);
      check(RestoreBridgeExceptions.countMissing(scalaPartial) == 1, "partial Scala fixture should lack 1 exception");
      checkRestored(RestoreBridgeExceptions.restore(scalaPartial));
    });
    run("3 already restored input is byte-for-byte unchanged (fixture, Scala, real class)", () -> {
      Map<String, Object[]> spec = spec();
      spec.put(OPEN, new Object[] {BRIDGE, IOE});
      spec.put(OPEN_BUFFERED, new Object[] {BRIDGE, IOE});
      byte[] in = fixture(spec);
      check(Arrays.equals(in, RestoreBridgeExceptions.restore(in)), "fixture bytes changed");
      byte[] scalaOut = RestoreBridgeExceptions.restore(scala);
      check(Arrays.equals(scalaOut, RestoreBridgeExceptions.restore(scalaOut)), "restored Scala fixture changed");
      check(Arrays.equals(real, RestoreBridgeExceptions.restore(real.clone())), "real ArrayByteSource changed");
      checkRestored(real);
    });
    run("4 restoring twice equals restoring once", () -> {
      for (byte[] in : Arrays.asList(fixture(spec()), scala)) {
        byte[] once = RestoreBridgeExceptions.restore(in);
        check(Arrays.equals(once, RestoreBridgeExceptions.restore(once)), "second restore changed bytes");
      }
    });
    run("5 concrete and unrelated methods keep flags and exceptions", () -> {
      for (byte[] in : Arrays.asList(fixture(spec()), scala, real)) {
        Map<String, String> before = shapes(in);
        Map<String, String> after = shapes(RestoreBridgeExceptions.restore(in));
        check(before.keySet().equals(after.keySet()), "method set changed");
        for (String key : before.keySet()) {
          if (!RestoreBridgeExceptions.BRIDGES.contains(key)) {
            check(before.get(key).equals(after.get(key)), "shape changed on " + key);
          }
        }
        for (String concrete : RestoreBridgeExceptions.CONCRETES) {
          check(after.get(concrete).endsWith(" []"), "concrete method declares exceptions: " + concrete);
        }
      }
      check(shapes(fixture(spec())).get("copyTo(Ljava/io/OutputStream;)J").endsWith(" [java/io/IOException]"),
          "unrelated throwing method lost its exception");
    });
    run("6 other bytecode is identical after normalisation", () -> {
      for (byte[] in : Arrays.asList(fixture(spec()), scala)) {
        check(Arrays.equals(normalise(in), normalise(RestoreBridgeExceptions.restore(in))),
            "bytecode differs beyond bridge exceptions");
      }
      byte[] stripped = setExceptions(setExceptions(real, OPEN, null), OPEN_BUFFERED, null);
      check(RestoreBridgeExceptions.countMissing(stripped) == 2, "stripped real class should lack 2 exceptions");
      byte[] restored = RestoreBridgeExceptions.restore(stripped);
      check(Arrays.equals(normalise(real), normalise(restored)), "real class differs beyond bridge exceptions");
      check(shapes(real).equals(shapes(restored)), "re-restored real class differs from processed class");
    });
    run("7 missing methods and unexpected shapes fail closed", () -> {
      expectFailure("missing bridge", fixture(with(OPEN, null)));
      expectFailure("missing concrete", fixture(with("openStream()Ljava/io/ByteArrayInputStream;", null)));
      expectFailure("bridge without ACC_SYNTHETIC",
          fixture(with(OPEN, new Object[] {Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE, NONE})));
      expectFailure("bridge with unexpected exception",
          fixture(with(OPEN, new Object[] {BRIDGE, Collections.singletonList("java/lang/Exception")})));
      expectFailure("bridge with extra exception",
          fixture(with(OPEN, new Object[] {BRIDGE, Arrays.asList("java/io/IOException", "java/lang/Exception")})));
      expectFailure("concrete declaring IOException",
          fixture(with("openBufferedStream()Ljava/io/ByteArrayInputStream;", new Object[] {Opcodes.ACC_PUBLIC, IOE})));
      expectFailure("unexpected overload",
          fixture(with("openStream(I)Ljava/io/InputStream;", new Object[] {Opcodes.ACC_PUBLIC, NONE})));
      expectFailure("wrong class", rename(fixture(spec()), "com/opengamma/strata/collect/io/Other"));
      Path dir = Files.createTempDirectory("restore-bridge-exceptions-control");
      Path file = dir.resolve(FILE);
      Files.createDirectories(file.getParent());
      byte[] bad = fixture(with(OPEN_BUFFERED, null));
      Files.write(file, bad);
      int exit = runMain("RestoreBridgeExceptions", dir);
      check(exit == 1, "main should exit 1 on bad shape but exited " + exit);
      check(Arrays.equals(bad, Files.readAllBytes(file)), "main modified the class file despite failing");
      deleteTree(dir);
    });

    for (String result : RESULTS) {
      System.out.println("RestoreBridgeExceptionsControls: " + result);
    }
    System.out.println("RestoreBridgeExceptionsControls: " + (RESULTS.size() - failures) + " of " + RESULTS.size() +
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
      RestoreBridgeExceptions.restore(bytes);
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

  private static void checkRestored(byte[] bytes) {
    Map<String, String> shapes = shapes(bytes);
    for (String bridge : RestoreBridgeExceptions.BRIDGES) {
      check(("0x" + Integer.toHexString(BRIDGE) + " [java/io/IOException]").equals(shapes.get(bridge)),
          "bridge not restored: " + bridge + " -> " + shapes.get(bridge));
    }
    for (String concrete : RestoreBridgeExceptions.CONCRETES) {
      check(shapes.get(concrete) != null && shapes.get(concrete).endsWith(" []"),
          "concrete method changed: " + concrete + " -> " + shapes.get(concrete));
    }
    check(RestoreBridgeExceptions.countMissing(bytes) == 0, "restored class still lacks exceptions");
  }

  // defines the restored Scala class in isolation and calls both bridges through Guava's ByteSource type
  private static void checkLoadsAndRuns(byte[] bytes) throws Exception {
    String name = RestoreBridgeExceptions.CLASS_NAME.replace('/', '.');
    ClassLoader loader = new ClassLoader(RestoreBridgeExceptionsControls.class.getClassLoader()) {
      @Override
      protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        if (className.equals(name)) {
          synchronized (getClassLoadingLock(className)) {
            Class<?> loaded = findLoadedClass(className);
            return loaded != null ? loaded : defineClass(className, bytes, 0, bytes.length);
          }
        }
        return super.loadClass(className, resolve);
      }
    };
    Class<?> cls = Class.forName(name, true, loader);
    Object instance = cls.getConstructor(byte[].class).newInstance((Object) new byte[] {1, 2, 3});
    Class<?> byteSource = Class.forName("com.google.common.io.ByteSource", false, loader);
    for (String methodName : Arrays.asList("openStream", "openBufferedStream")) {
      Method viaSuper = byteSource.getMethod(methodName);
      try (InputStream in = (InputStream) viaSuper.invoke(instance)) {
        check(in.read() == 1, "unexpected stream content from " + methodName);
      }
      for (Method method : cls.getDeclaredMethods()) {
        if (method.getName().equals(methodName) && method.isBridge()) {
          check(Arrays.equals(method.getExceptionTypes(), new Class<?>[] {IOException.class}),
              "reflected bridge exceptions wrong on " + methodName);
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  // name+descriptor -> "0xflags [exceptions]"
  private static Map<String, String> shapes(byte[] bytes) {
    Map<String, String> result = new LinkedHashMap<>();
    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        result.put(name + desc, "0x" + Integer.toHexString(access) + " " +
            (exceptions == null ? NONE : Arrays.asList(exceptions)));
        return null;
      }
    }, ClassReader.SKIP_CODE);
    return result;
  }

  // rebuilds the class from scratch without bridge exceptions, so constant pool layout cannot hide differences
  private static byte[] normalise(byte[] bytes) {
    ClassWriter writer = new ClassWriter(0);
    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        boolean bridge = RestoreBridgeExceptions.BRIDGES.contains(name + desc);
        return super.visitMethod(access, name, desc, signature, bridge ? null : exceptions);
      }
    }, 0);
    return writer.toByteArray();
  }

  // no ClassReader is passed to the writer, as ASM would then keep the source Exceptions attribute when removing it
  private static byte[] setExceptions(byte[] bytes, String target, String[] newExceptions) {
    ClassWriter writer = new ClassWriter(0);
    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return super.visitMethod(access, name, desc, signature, target.equals(name + desc) ? newExceptions : exceptions);
      }
    }, 0);
    return writer.toByteArray();
  }

  private static byte[] rename(byte[] bytes, String newName) {
    ClassWriter writer = new ClassWriter(0);
    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9, writer) {
      @Override
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, newName, signature, superName, interfaces);
      }
    }, 0);
    return writer.toByteArray();
  }

  //-------------------------------------------------------------------------
  // name+descriptor -> {access, exceptions}; the default is the Scala 3 shape with exception-free bridges
  private static Map<String, Object[]> spec() {
    Map<String, Object[]> spec = new LinkedHashMap<>();
    spec.put("<init>([B)V", new Object[] {Opcodes.ACC_PUBLIC, NONE});
    spec.put("openStream()Ljava/io/ByteArrayInputStream;", new Object[] {Opcodes.ACC_PUBLIC, NONE});
    spec.put("openBufferedStream()Ljava/io/ByteArrayInputStream;", new Object[] {Opcodes.ACC_PUBLIC, NONE});
    spec.put("size()J", new Object[] {Opcodes.ACC_PUBLIC, NONE});
    spec.put("copyTo(Ljava/io/OutputStream;)J", new Object[] {Opcodes.ACC_PUBLIC, IOE});
    spec.put(OPEN, new Object[] {BRIDGE, NONE});
    spec.put(OPEN_BUFFERED, new Object[] {BRIDGE, NONE});
    return spec;
  }

  private static Map<String, Object[]> with(String key, Object[] value) {
    Map<String, Object[]> spec = spec();
    if (value == null) {
      spec.remove(key);
    } else {
      spec.put(key, value);
    }
    return spec;
  }

  @SuppressWarnings("unchecked")
  private static byte[] fixture(Map<String, Object[]> spec) {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
        RestoreBridgeExceptions.CLASS_NAME, null, "com/google/common/io/ByteSource", null);
    for (Map.Entry<String, Object[]> entry : spec.entrySet()) {
      String key = entry.getKey();
      int paren = key.indexOf('(');
      String name = key.substring(0, paren);
      String desc = key.substring(paren);
      List<String> exceptions = (List<String>) entry.getValue()[1];
      MethodVisitor mv = cw.visitMethod((Integer) entry.getValue()[0], name, desc, null,
          exceptions.isEmpty() ? null : exceptions.toArray(new String[0]));
      mv.visitCode();
      if (name.equals("<init>")) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/google/common/io/ByteSource", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
      } else if (desc.endsWith(")J")) {
        mv.visitInsn(Opcodes.LCONST_0);
        mv.visitInsn(Opcodes.LRETURN);
      } else if (desc.equals("()Ljava/io/InputStream;")) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, RestoreBridgeExceptions.CLASS_NAME, name,
            "()Ljava/io/ByteArrayInputStream;", false);
        mv.visitInsn(Opcodes.ARETURN);
      } else {
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
      }
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
    cw.visitEnd();
    return cw.toByteArray();
  }

}
