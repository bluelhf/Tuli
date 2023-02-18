<img align="right" width="10%" src="assets/logo.png"/></img>

# Tuli
> From the spark, O Bride of Beauty, light thy fires.

Tuli (/ˈtuli/) is a library for accessing strongly encapsulated JDK internals
and other sealed classes from _within_ named modules, as opposed to performing
such access operations from unnamed modules.

Tuli's goal is to address issues with existing mechanisms for breaking the
encapsulation of sealed classes. As even obtaining a `Class<?>` instance
for a sealed class from within a named module is often impossible,
traditional methods of JVM privilege escalation do not work.

## Requirements
- The running JVM must be capable of **[instrumentation](https://docs.oracle.com/en/java/javase/17/docs/api/java.instrument/java/lang/instrument/package-summary.html)**
- The filesystem must be capable of creating temporary directories and files in them
  for Tuli to actually write and load its agent
- The Java process must have sufficient privileges to spawn at least one other
  Java process with the same executable file as the initial process
- Java 17+

## Examples
<sub>Sufficient imports, dependencies, and module requirement declarations are assumed.</sub>
### [jOOR](https://github.com/jOOQ/jOOR) on Tuli
```java
System.err.printf("Currently in %s%n", currentModule());

Tuli.open(javaBase(), currentModule(), module("org.jooq.joor"));

Reflect.onClass("jdk.internal.misc.Unsafe")
        .call("getUnsafe")
        .call("putInt", null, 0L, 0);
```
#### Result
```
Currently in module blue.lhf.tuli
#
# A fatal error has been detected by the Java Runtime Environment:
#
#  SIGSEGV (0xb) at pc=0x00007fcb8ed33edb, pid= ...
```

### [Mirror](https://github.com/Moderocky/Mirror) on Tuli
<sub>(This demonstration does not seg-fault,
because [as of 2022-09-01], Mirror does not support
long/double parameters in its method accessors)</sub>
```java
System.err.printf("Currently in %s%n", currentModule());
Tuli.open(javaBase(), currentModule());

/*
 * Mirror uses a different module for its accessors,
 * we need to explicitly specify access using Tuli's
 * <T> T withAccess(T, Module...) helper method
 * */
final Object unsafe =
    Tuli.withAccess(Mirror.of(Class.forName("jdk.internal.misc.Unsafe"))
            .method("getUnsafe"), javaBase()).invoke();

System.err.printf("Unsafe: %s%n", unsafe);
```
#### Result
```
Currently in module blue.lhf.tuli
Unsafe: jdk.internal.misc.Unsafe@161cd475
```
