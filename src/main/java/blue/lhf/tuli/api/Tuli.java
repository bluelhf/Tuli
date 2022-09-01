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

    /**
     * Opens all packages in the target module to the given module.
     * @param target The module whose packages should be opened.
     * @param to The module to open the target module's packages to.
     * @see Tuli#open(Module, Module...)
     * @see Tuli#open(Set, Set)
     * */
    public static void open(final Module target, final Module to) {
        open(singleton(target), singleton(to));
    }

    /**
     * Opens all packages in the target module to the given modules.
     * @param target The module whose packages should be opened.
     * @param to The modules to open the target module's packages to.
     * @see Tuli#open(Module, Module)
     * @see Tuli#open(Set, Set)
     * */
    public static void open(final Module target, final Module... to) {
        open(singleton(target), Set.of(to));
    }

    /**
     * Opens all packages in the target modules to the given modules.
     * @param targets The module whose packages should be opened.
     * @param to The modules to open the target module's packages to.
     * @see Tuli#open(Module, Module)
     * @see Tuli#open(Set, Set)
     * */
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

    /**
     * Utility method that opens all packages in the given object's class's module to the given modules and then returns the object.
     * @param <T> The type of the object
     * @param object The object whose class's module's packages should be opened.
     * @param toModules The modules to which the given object's class's module's packages should be opened.
     * @return The same object.
     * */
    public static <T> T withAccess(final T object, final Module... toModules) {
        open(Set.of(toModules), singleton(moduleOf(object)));
        return object;
    }
}
