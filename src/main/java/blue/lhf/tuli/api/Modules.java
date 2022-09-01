package blue.lhf.tuli.api;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

/**
 * Utility class for obtaining modules to open.
 * */
public class Modules {
    private Modules() {
    }

    /**
     * @return The module of the caller class. Uses {@link StackWalker}.
     * */
    public static Module currentModule() {
        return StackWalker.getInstance(RETAIN_CLASS_REFERENCE)
            .getCallerClass().getModule();
    }

    /**
     * @return The <i>java.base</i> module, more specifically the module of the {@link Object} class.
     * */
    public static Module javaBase() {
        return Object.class.getModule();
    }

    /**
     * @return The module of the class that the given object is an instance of.
     * */
    public static <T> Module moduleOf(final T object) {
        return object.getClass().getModule();
    }

    /**
     * Attempts to find the given module in the 'boot' module layer.
     * The discovery process for this layer can be found in <a href="https://github.com/openjdk/jdk/blob/0971d3464609bf4124df460ea73ff761d7e0f7b2/src/java.base/share/classes/jdk/internal/module/ModuleBootstrap.java#L182"><code>java.base/jdk.internal.module.ModuleBootstrap.boot2()</code></a>
     *
     * @param name The name of the module to look for.
     * @return The module with the given name if it can be found in {@link ModuleLayer#boot()}.
     * */
    public static Module module(final String name) {
        return ModuleLayer.boot().findModule(name).orElse(null);
    }
}
