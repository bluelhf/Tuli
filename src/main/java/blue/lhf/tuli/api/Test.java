package blue.lhf.tuli.api;

import mx.kenzie.mirror.*;

import static blue.lhf.tuli.api.Modules.*;

public class Test {
    public static void main(String[] args) throws ClassNotFoundException {
        System.err.printf("Currently in %s%n", currentModule());
        Tuli.open(javaBase(), currentModule());

        /*
        * Mirror uses a different module for its accessors,
        * we need to explicitly specify access using Tuli's
        * <T> T withAccess(T, Module...) helper method
        * */
        final Object unsafe = Tuli.withAccess(
                Mirror.of(Class.forName("jdk.internal.misc.Unsafe"))
                    .method("getUnsafe"),
            javaBase()).invoke();

        System.err.printf("Unsafe: %s%n", unsafe);
    }
}
