package blue.lhf.tuli.api;

import blue.lhf.tuli.impl.SneakyDevil;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.jar.*;

import static blue.lhf.tuli.api.Modules.moduleOf;
import static java.lang.ProcessHandle.current;
import static java.util.Collections.singleton;

@SuppressWarnings("unused")
public class Tuli {
    private Tuli() {
    }

    private static Instrumentation instrumentation;

    public static void open(final Module target, final Module to) {
        open(singleton(target), singleton(to));
    }

    public static void open(final Module target, final Module... to) {
        open(singleton(target), Set.of(to));
    }

    public static void open(final Set<Module> targets, final Set<Module> to) {
        for (final Module module : targets) {
            final Map<String, Set<Module>> extras = new HashMap<>();
            for (final String pkg : module.getPackages()) {
                extras.put(pkg, to);
            }

            if (instrumentation == null) instrument().join();

            instrumentation.redefineModule(module, Set.of(),
                extras, extras, Set.of(), Map.of());
        }
    }

    public static <T> T withAccess(final T object, final Module... toModules) {
        open(Set.of(toModules), singleton(moduleOf(object)));
        return object;
    }

    public static void instrument(final Instrumentation instrumentation) {
        if (!isInstrumented()) Tuli.instrumentation = instrumentation;
    }

    /**
     * Writes a JAR file in
     */
    private static Path buildJAR() throws IOException {
        final Path temp = Files.createTempDirectory("tuli");
        final Path target = temp.resolve("tuli.jar");

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        manifest.getMainAttributes().putValue("Main-Class", SneakyDevil.class.getCanonicalName());
        manifest.getMainAttributes().putValue("Agent-Class", SneakyDevil.class.getCanonicalName());


        final Class<?>[] toWrite = new Class[]{Tuli.class, SneakyDevil.class};
        try (final JarOutputStream out = new JarOutputStream(Files.newOutputStream(target), manifest)) {
            for (final Class<?> clazz : toWrite) {
                final String path = clazz.getCanonicalName().replace('.', '/') + ".class";
                out.putNextEntry(new JarEntry(path));

                final URL resource = clazz.getClassLoader().getResource(path);
                if (resource == null) throw new IllegalStateException("Tuli is corrupt" +
                    " or class loader is unusual, failed to find " + path);

                try (final BufferedInputStream stream = new BufferedInputStream(resource.openStream())) {
                    out.write(stream.readAllBytes());
                }

                out.closeEntry();
            }
        }

        return target;
    }

    public static CompletableFuture<Void> instrument() throws InstrumentException {
        if (isInstrumented()) return CompletableFuture.completedFuture(null);
        try {
            final Path jar = buildJAR();
            final String jarPath = jar.toAbsolutePath().toString();
            return new ProcessBuilder(
                current().info().command().orElseThrow(),
                "-jar", jarPath,
                current().pid() + "", jarPath
            ).start().onExit().thenRun(() -> {});
        } catch (IOException ioe) {
            throw new InstrumentException("Failed to instrument JVM", ioe);
        }
    }

    public static boolean isInstrumented() {
        return Tuli.instrumentation != null;
    }
}
