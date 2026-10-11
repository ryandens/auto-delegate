# AutoDelegate

[![Validate](https://github.com/ryandens/auto-delegate/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/ryandens/auto-delegate/actions/workflows/gradle.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.ryandens/auto-delegate-annotations/badge.svg#)](https://maven-badges.herokuapp.com/maven-central/com.ryandens/auto-delegate-annotations)

Java annotation processor for automatically delegating interface APIs to a composed instance of that interface. This
project was inspired by Google's <a href="https://github.com/google/auto">auto</a> project and leverages utilities
exposed in <a href="https://github.com/google/auto/tree/master/common">
auto-common</a>.

Intro blog post: [https://www.ryandens.com/post/auto_delegate/](https://www.ryandens.com/post/auto_delegate/)

## Usage

Requirements:

- JDK 11 or above

### Gradle

```kotlin
dependencies {
    compileOnly("com.ryandens", "auto-delegate-annotations", "0.3.1")
    annotationProcessor("com.ryandens", "auto-delegate-processor", "0.3.1")
}
```

### Maven

```xml

<project>
    <dependencies>
        <dependency>
            <groupId>com.ryandens</groupId>
            <artifactId>auto-delegate-annotations</artifactId>
            <version>${auto-delegate.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>com.ryandens</groupId>
                            <artifactId>auto-delegate-processor</artifactId>
                            <version>${auto-delegate.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Simple example

This simple example usage of `@AutoDelegate` is based off of the example given
in [kotlin's delegation documentation](https://kotlinlang.org/docs/delegation.html). There are more examples in [auto-delegate-examples](./auto-delegate-examples/src/main/java/com/ryandens/delegation/examples)

```java
public interface Base {
    void print();
}

public final class BaseImpl implements Base {
    private final int x;

    public BaseImpl(final int x) {
        this.x = x;
    }

    @Override
    void print() {
        System.out.println(x);
    }
}

@AutoDelegate(Base.class)
final class Derived extends AutoDelegate_Derived implements Base {

    Derived(final Base base) {
        super(base);
    }
}

```

## Why?

Decorates a class with metadata to describe an abstract parent class that automatically delegates to an inner composed
instance of an interface. This annotation processor is inspired by the Kotlin language
feature <a href="https://kotlinlang.org/docs/delegation.html">delegation</a>.

The goal of this is to encourage the use of composition over inheritance as described by Effective Java Item 18 "Favor
composition over inheritance". In the section of the book, Bloch describes an `InstrumentedSet`that counts the number of
items added to it. In order to accomplish this, Bloch creates an abstract implementation of `java.util.Set`
called `ForwardingSet` that simply composes a `java.util.Set` instance and forwards all calls to it. This allows Bloch
to write the `InstrumentedSet` in a less verbose manner, by extending `ForwardingSet` and overriding the "add" related
methods for instrumentation purposes. This is a great solution in the context of Java, but Kotlin lowers the cognitive
barrier of using composition by making it less verbose to do so. In Kotlin, the need for a `ForwardingSet`is obviated by
the "delegation" language feature linked above. The `InstrumentedSet` can be written concisely without relying on
writing a `ForwardingSet` like:

```kotlin 

 class InstrumentedSet<E>(val inner: MutableSet<E>) : MutableSet<E> by inner {
     var count: Int = 0

     override fun add(element: E): Boolean {
         count++
         return inner.add(element)
     }

     override fun addAll(elements: Collection<E>) : Boolean {
         count += elements.size
         return inner.addAll(elements)
     }
 }
```

This annotation strives to enable developers in the same fashion by generating abstract `Forwarding` classes that
delegate to the inner composed instance. An equivalent `InstrumentedSet` implementation written with `AutoDelegate` is

```java

@AutoDelegate(Set.class)
public final class InstrumentedSet<E> extends AutoDelegate_InstrumentedSet<E> implements Set<E> {
    private int addCount;

    public InstrumentedSet(final Set<E> inner) {
        super(inner);
        this.addCount = 0;
    }

    @Override
    public boolean add(final E t) {
        addCount++;
        return super.add(t);
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c);
    }

    /**
     * @return the number of times a caller has attempted to add an item to this set
     */
    public int addCount() {
        return addCount;
    }
}
```

While this is not as concise as the Kotlin implementation, it generates a class called `AutoDelegate_InstrumentedSet` in
the same package as the declaring class. The declared class can then extend the generated class and call `super`
APIs where appropriate, only overriding methods that are relevant to the implementation

## Internals

### 👩‍💻 Development Requirements

- JDK 27

### 🚀 Releasing

The project version lives in root `gradle.properties`. Push a matching `X.Y.Z`
tag from a commit on `main` to validate, sign, publish both library modules to
Maven Central, and create a GitHub release.

See [the release guide](docs/releasing.md) for the four GitHub environment secrets,
Keybase/1Password setup, and recovery instructions.

## Building

Run `./gradlew build :build-logic:check --warning-mode fail` to build and test all
modules and validate the convention plugins. The `build-logic` included build
provides shared formatting, metadata, publishing, and Java/JUnit conventions.
Dependency versions live in `gradle/libs.versions.toml`, shared with `build-logic`;
dependency repositories are declared in each build's settings. No `buildSrc` is required.
The build-logic checks include TestKit coverage for publishing artifacts, plugin
application order, configuration-cache reuse, and build-cache reuse.
Install JDK 27 before building. The Gradle daemon and compilation use JDK 27
through toolchains, with automatic JDK downloads disabled so missing local or CI
setup fails explicitly. CI installs Temurin 27 through `actions/setup-java`.
Production code (including examples) compiles with `--release 11`; tests compile
and run on JDK 27 using the toolchain defaults. JUnit dependencies are aligned
through the JUnit BOM. The processor test also verifies generated code with
`--release 11`.
The included build also compiles with JDK 27, targeting Java 17 for Gradle plugin
compatibility because the bundled Kotlin compiler cannot emit Java 27 bytecode.
Regenerate daemon criteria with `./gradlew updateDaemonJvm`; no download URLs
are generated.

Develocity publishes build scans automatically when `CI=true`. For a local scan,
run `./gradlew build --scan`; use `--no-scan` to skip publication. Scans are
published to Gradle's public service under its configured terms of use, so avoid
including sensitive build information.
