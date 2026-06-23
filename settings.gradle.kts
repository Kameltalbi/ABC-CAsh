// Register Homebrew OpenJDK 17 when macOS java_home does not see it.
sequenceOf(
    "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home",
    "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
)
    .map { java.io.File(it) }
    .firstOrNull { it.isDirectory }
    ?.let { jdkHome ->
        val existing = System.getProperty("org.gradle.java.installations.paths")
        val paths = if (existing.isNullOrBlank()) jdkHome.absolutePath else "$existing,${jdkHome.absolutePath}"
        System.setProperty("org.gradle.java.installations.paths", paths)
    }

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "ABCCash"
include(":app")
include(":server")
