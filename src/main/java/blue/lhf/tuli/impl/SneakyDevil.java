package blue.lhf.tuli.impl;

import blue.lhf.tuli.api.*;
import com.sun.tools.attach.*;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.jar.*;

import static java.lang.ProcessHandle.current;

/**
 * A sneaky implementation detail tasked with instrumenting arbitrary JVMs and allowing {@link Tuli} to access
 * the instrumentation freely.
 * */
public class SneakyDevil {

    public static Instrumentation instrumentation;

    /**
     * Calls {@link SneakyDevil#instrument(Instrumentation)} with the given {@link Instrumentation}.
     * @param args Useless arguments, needed to adhere to agentmain method signature
     * @param instrumentation The instrumentation to pass to {@link SneakyDevil#instrument(Instrumentation)}
     * */
    public static void agentmain(final String args, Instrumentation instrumentation) {
        instrument(instrumentation);
    }

    /**
     * Sets the {@link SneakyDevil}.{@link SneakyDevil#instrumentation} field to the given value if it is not set.
     * @param instrumentation The instrumentation to set the field to.
     * */
    public static void instrument(final Instrumentation instrumentation) {
        if (!isInstrumented()) SneakyDevil.instrumentation = instrumentation;
    }

    /**
     * @return Whether the current JVM has been instrumented or not.
     * */
    public static boolean isInstrumented() {
        return instrumentation != null;
    }

    /**
     * Builds a Java archive in an operating system provided temporary directory to be used
     * for instrumenting the current JVM. This archive will have a manifest which declares
     * that {@link SneakyDevil} is the main class and the agent main class of the archive.
     * <p>
     * This method is used by {@link SneakyDevil#instrumentProcess(long)} to build a new
     * process which then instruments the desired JVM, setting the VM's {@link SneakyDevil}'s {@link SneakyDevil#instrumentation} field.
     * */
    private static Path buildJAR() throws IOException {
        final Path temp = Files.createTempDirectory("tuli");
        final Path target = temp.resolve("tuli.jar");

        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        manifest.getMainAttributes().putValue("Main-Class", SneakyDevil.class.getCanonicalName());
        manifest.getMainAttributes().putValue("Agent-Class", SneakyDevil.class.getCanonicalName());


        // SneakyDevil and its non-Java SE dependencies
        final Class<?>[] toWrite = new Class[]{Tuli.class, SneakyDevil.class};

        try (final JarOutputStream out = new JarOutputStream(Files.newOutputStream(target), manifest)) {
            for (final Class<?> clazz : toWrite) {
                final String path = clazz.getCanonicalName().replace('.', '/') + ".class";
                out.putNextEntry(new JarEntry(path));

                final URL resource = clazz.getClassLoader().getResource(path);
                if (resource == null) throw new IllegalStateException("SneakyDevil is corrupt" +
                    " or class loader is unusual, failed to find " + path);

                try (final BufferedInputStream stream = new BufferedInputStream(resource.openStream())) {
                    out.write(stream.readAllBytes());
                }

                out.closeEntry();
            }
        }

        return target;
    }

    /**
     * Instruments the desired JVM (yes, even the current one) by creating a new JVM tasked
     * with attaching the SneakyDevil java agent onto the JVM specified by the process ID parameter.
     * @param pid The {@link ProcessHandle#of(long)} process ID representing the JVM that is to be instrumented.
     * @return A {@link CompletableFuture<Void>} that completes when instrumentation is complete. The instrumented JVM's
     * {@link SneakyDevil}'s {@link SneakyDevil#instrumentation} field will be set.
     * */
    public static CompletableFuture<Void> instrumentProcess(final long pid) throws InstrumentException {
        try {
            final Path jar = buildJAR();
            final String jarPath = jar.toAbsolutePath().toString();
            return new ProcessBuilder(
                current().info().command().orElseThrow(),
                "-jar", jarPath,
                pid + "", jarPath
            ).start().onExit().thenRun(() -> {});
        } catch (IOException ioe) {
            throw new InstrumentException("Failed to instrument JVM", ioe);
        }
    }

    /**
     * Attaches to the PID given by the first String argument the agent Java archive given by the second String argument.
     * <p>
     * Runs in the instrumenting JVM spawned by {@link SneakyDevil#instrumentProcess(long)}, should not be used.
     * */
    public static void main(String[] args) throws IOException,
        AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        VirtualMachine.attach(args[0]).loadAgent(args[1]);
    }


    /**
     * @return An {@link Optional<Instrumentation>} representing the instrumentation or an empty optional if the current
     * JVM is not instrumented by {@link SneakyDevil}.
     * */
    public static Optional<Instrumentation> getInstrumentation() {
        return Optional.ofNullable(instrumentation);
    }

    /**
     * Instruments the current JVM using {@link SneakyDevil#instrumentProcess(long)} if it is not already instrumented.
     * @return A {@link CompletableFuture<Void>} that returns when the instrumentation is complete, or immediately if
     * the JVM is already instrumented.
     * @see SneakyDevil#instrumentProcess(long)
     * */
    public static CompletableFuture<Void> instrument() throws InstrumentException {
        if (isInstrumented()) return CompletableFuture.completedFuture(null);
        return SneakyDevil.instrumentProcess(ProcessHandle.current().pid());
    }

    /**
     * Instruments the current JVM if it has not been instrumented already, and returns the obtained instrumentation.
     * @return The instrumentation.
     * */
    public static Instrumentation obtainInstrumentation() {
        instrument().join();
        return instrumentation;
    }
}
