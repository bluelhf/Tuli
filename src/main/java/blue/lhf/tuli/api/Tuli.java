package blue.lhf.tuli.api;

import blue.lhf.tuli.impl.SneakyDevil;

import java.lang.instrument.Instrumentation;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static blue.lhf.tuli.api.Modules.moduleOf;
import static java.util.Collections.singleton;

@SuppressWarnings("unused")
public class Tuli {
    private Tuli() {
    }

    public static void open(final Module target, final Module to) {
        open(singleton(target), singleton(to));
    }

    public static void open(final Module target, final Module... to) {
        open(singleton(target), Set.of(to));
    }

    public static void open(final Set<Module> targets, final Set<Module> to) {
        Instrumentation instrumentation = SneakyDevil.getInstrumentation().orElse(null);
        for (final Module module : targets) {
            final Map<String, Set<Module>> extras = new HashMap<>();
            for (final String pkg : module.getPackages()) {
                extras.put(pkg, to);
            }

            if (instrumentation == null)
                instrumentation = SneakyDevil.obtainInstrumentation();

            instrumentation.redefineModule(module, Set.of(),
                extras, extras, Set.of(), Map.of());
        }
    }

    public static <T> T withAccess(final T object, final Module... toModules) {
        open(Set.of(toModules), singleton(moduleOf(object)));
        return object;
    }
}
