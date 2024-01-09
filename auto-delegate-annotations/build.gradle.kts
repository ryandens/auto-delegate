plugins {
    `java-library`
    id("com.ryandens.delegation.publish")
}

description =
    """
    Annotations and metadata for decorating classes to enable the generation of auto-delegating abstract implementations of interfaces.
    """.trimIndent()

tasks.compileJava {
    options.release.set(11)
}
