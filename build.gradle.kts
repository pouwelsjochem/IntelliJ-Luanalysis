import de.undercouch.gradle.tasks.download.Download
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.ChangelogPluginExtension
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

val intellijPlatformType = providers.gradleProperty("intellijPlatform.type").orElse("IC")
val intellijPlatformVersion = providers.gradleProperty("intellijPlatform.version")
val intellijPlatformBundledPlugins = providers.gradleProperty("intellijPlatform.bundledPlugins")
    .map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
    .orElse(emptyList())
val intellijPlatformBundledModules = providers.gradleProperty("intellijPlatform.bundledModules")
    .map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
    .orElse(emptyList())

val pluginVersionProvider = providers.gradleProperty("pluginVersion")
val pluginNameProvider = providers.gradleProperty("pluginName")
val pluginSinceBuildProvider = providers.gradleProperty("pluginSinceBuild")
val pluginVerifierIdeVersions = providers.gradleProperty("pluginVerifierIdeVersions")
    .map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
    .orElse(emptyList())

val changelogExtension: ChangelogPluginExtension = extensions.getByType(ChangelogPluginExtension::class.java)
val changeNotesProvider = providers.provider {
    val version = pluginVersionProvider.orNull
    val changelogItem = version
        ?.takeIf { changelogExtension.has(it) }
        ?.let { changelogExtension.getOrNull(it) }
        ?: runCatching { changelogExtension.getLatest() }.getOrNull()
        ?: changelogExtension.getUnreleased()

    changelogExtension.renderItem(changelogItem, Changelog.OutputType.HTML)
}

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("org.jetbrains.intellij.platform") version "2.9.0"
    id("org.jetbrains.changelog") version "2.0.0"
    id("de.undercouch.download") version "5.6.0"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
    mavenCentral()
    gradlePluginPortal()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(fileTree("libs") {
        include("*.jar")
    })

    implementation(kotlin("stdlib", "2.2.0"))

    intellijPlatform {
        create(intellijPlatformType, intellijPlatformVersion)
        intellijPlatformBundledPlugins.orNull?.takeIf { it.isNotEmpty() }?.let { bundledPlugins(it) }
        intellijPlatformBundledModules.orNull?.takeIf { it.isNotEmpty() }?.let { bundledModules(it) }
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test", "2.2.0"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

changelog {
    version.set(properties("pluginVersion"))
    groups.set(emptyList<String>())
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

intellijPlatform {
    pluginConfiguration {
        name.set(pluginNameProvider)
        version.set(pluginVersionProvider)
        changeNotes.set(changeNotesProvider)

        ideaVersion {
            sinceBuild.set(pluginSinceBuildProvider.orNull)
        }
    }

    pluginVerification {
        val resolvedType = intellijPlatformType.get()
        pluginVerifierIdeVersions.orNull?.forEach { version ->
            ides.create(resolvedType, version)
        }
    }
}

sourceSets {
    main {
        java {
            srcDirs("gen")
        }
        resources {
            exclude("debugger/**")
        }
    }
}
tasks {
    withType<JavaExec> {
        jvmArgs = listOf("-Xms2048m", "-Xmx8192m")
    }

    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.set(
                listOf(
                    "-Xjvm-default=all",
                    "-opt-in=kotlin.contracts.ExperimentalContracts"
                )
            )
        }
    }

    patchPluginXml {
        dependsOn("copyEmmyLuaDebugger")
        pluginVersion.set(pluginVersionProvider)
        changeNotes.set(changeNotesProvider)
    }

    val debuggerArchitectures = arrayOf("x86", "x64")

    register<Download>("downloadEmmyLuaDebugger") {
        val debuggerVersion = properties("emmyLuaDebuggerVersion")

        src(arrayOf(
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${debuggerVersion}/emmy_core.so",
            "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${debuggerVersion}/emmy_core.dylib",
            *debuggerArchitectures.map {
                "https://github.com/EmmyLua/EmmyLuaDebugger/releases/download/${debuggerVersion}/emmy_core@${it}.zip"
            }.toTypedArray()
        ))

        dest("temp")
    }

    register<Copy>("extractEmmyLuaDebugger") {
        dependsOn("downloadEmmyLuaDebugger")

        debuggerArchitectures.forEach {
            from(zipTree("temp/emmy_core@${it}.zip")) {
                into(it)
            }
        }

        destinationDir = file("temp")
    }

    register<Copy>("copyEmmyLuaDebugger") {
        dependsOn("extractEmmyLuaDebugger")

        // Windows
        debuggerArchitectures.forEach {
            from("temp/${it}/") {
                into("debugger/emmy/windows/${it}")
            }
        }

        // Linux
        from("temp") {
            include("emmy_core.so")
            into("debugger/emmy/linux")
        }

        // Mac
        from("temp") {
            include("emmy_core.dylib")
            into("debugger/emmy/mac")
        }

        destinationDir = file("src/main/resources")
    }

    buildPlugin {
        dependsOn("copyEmmyLuaDebugger")

        val resourcesDir = "src/main/resources"

        from(fileTree(resourcesDir)) {
            include("debugger/**")
            into("/${project.name}/classes")
        }

        from(fileTree(resourcesDir)) {
            include("!!DONT_UNZIP_ME!!.txt")
            into("/${project.name}")
        }
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        channels.set(listOf(
            properties("pluginVersion")
                .split("-")
                .getOrElse(1) { "default" }
                .split(".")
                .first()
        ))
    }
}
