package blue.lhf.tuli;

import blue.lhf.tuli.api.*;

import java.util.concurrent.CompletableFuture;

import static blue.lhf.tuli.api.Modules.currentModule;
import static blue.lhf.tuli.api.Modules.javaBase;
import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.joor.Reflect.onClass;

public class TuliTest {
    private static final String UNSAFE_NAME = "jdk.internal.misc.Unsafe";

    public void disabledTestModule() {
        assert "blue.lhf.tuli.test".equals(getClass().getModule().getName()):
            "Tests are running in the wrong module!";
    }

    public final void testInstrumentation() {
        final CompletableFuture<?> future; (future = Tuli.instrument().orTimeout(5, SECONDS)).join();
        assert !future.isCompletedExceptionally() : "Instrumentation timed out.";
        assert Tuli.isInstrumented() : "Instrumentation failed.";
    }

    public final void testAccess() {
        Tuli.instrument().join();
        Tuli.open(javaBase(), currentModule());

        try {
            final Class<?> targetClass = Class.forName(UNSAFE_NAME);
            lookup().findStatic(targetClass, "getUnsafe", methodType(targetClass)).invoke();
        } catch (Throwable e) {
            assert false : "Accessing Unsafe threw "
                + e.getClass().getSimpleName();
        }
    }
}
