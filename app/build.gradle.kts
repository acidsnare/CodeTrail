import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvm()

    // Android: this module is a library; the :android module wraps it into the APK.
    androidLibrary {
        namespace = "codetrail.app"
        compileSdk = 37
        minSdk = 26
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "codetrail.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        // Everything a player sees lives here: screens, board art, game and app state, sound synthesis.
        // Platform code (window, files, audio device, locale) sits in the target source sets.
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
        }
        jvmMain {
            // The runtime window icon comes from the repo-level assets; installer icons stay out of the jar.
            resources.srcDir(rootProject.file("assets"))
            resources.exclude("icon/*.icns", "icon/*.ico", "icon/*.svg", "icon/*_1024.png")
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "codetrail.app.res"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "codetrail.app.MainKt"
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        // -Xdock:name fixes the "java" dock tooltip when running from Gradle. It is a macOS-only
        // flag: on Windows or Linux an unknown -X option stops the JVM from starting at all.
        if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) jvmArgs("-Xdock:name=CodeTrail")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "CodeTrail"
            packageVersion = "1.5.1"
            description = "Program your hero's path"
            vendor = "CodeTrail"
            macOS {
                iconFile.set(rootProject.file("assets/icon/codetrail.icns"))
                bundleID = "dk.codetrail.app"
                dockName = "CodeTrail"
            }
            windows {
                iconFile.set(rootProject.file("assets/icon/codetrail.ico"))
                menu = true
                menuGroup = "CodeTrail"
                shortcut = true
                // Machine-wide install into Program Files. Needs an admin prompt, but per-user
                // installs into AppData look like malware droppers to antivirus heuristics.
                perUserInstall = false
                dirChooser = true
                upgradeUuid = "4a2d1c5e-7b3f-4e8a-9c1d-2f6b8e0a7d31"
            }
            linux { iconFile.set(rootProject.file("assets/icon/codetrail_512.png")) }
        }
    }
}

tasks.register<JavaExec>("snapshot") {
    group = "verification"
    description = "Render the app to a PNG without a window: -Pout=file.png -Ptier=3 -Pseed=7"
    val main = kotlin.jvm().compilations.getByName("main")
    dependsOn(main.compileTaskProvider)
    classpath = main.output.allOutputs + main.runtimeDependencyFiles
    mainClass.set("codetrail.app.SnapshotKt")
    jvmArgs("--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true")
    args = listOf(
        providers.gradleProperty("out").getOrElse("build/snapshot.png"),
        providers.gradleProperty("tier").getOrElse("3"),
        providers.gradleProperty("seed").getOrElse("7"),
        if (providers.gradleProperty("solve").isPresent) "solve" else if (providers.gradleProperty("fail").isPresent) "fail" else if (providers.gradleProperty("hint").isPresent) "hint" else if (providers.gradleProperty("dnd").isPresent) "dnd" else "edit",
        providers.gradleProperty("world").getOrElse("islands"),
        providers.gradleProperty("lang").getOrElse("en"),
        providers.gradleProperty("stars").getOrElse("0"),
        providers.gradleProperty("screen").getOrElse("game"),
        providers.gradleProperty("frame").getOrElse("16"),
        providers.gradleProperty("hero").getOrElse("turtle"),
        providers.gradleProperty("mode").getOrElse("forward"),
    )
}

tasks.register<JavaExec>("renderIcon") {
    group = "build"
    description = "Render assets/icon/A_face.svg to transparent PNGs and an .icns"
    val main = kotlin.jvm().compilations.getByName("main")
    dependsOn(main.compileTaskProvider)
    classpath = main.output.allOutputs + main.runtimeDependencyFiles
    mainClass.set("codetrail.app.IconRenderKt")
    jvmArgs("--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true")
    args = listOf(
        rootProject.file(providers.gradleProperty("svg").getOrElse("assets/icon/A_face.svg")).path,
        rootProject.file(providers.gradleProperty("png").getOrElse("assets/icon/codetrail_1024.png")).path,
        providers.gradleProperty("size").getOrElse("1024"),
    )
}

tasks.register<JavaExec>("renderSounds") {
    group = "verification"
    description = "Write all synthesized sound effects to build/sounds/*.wav"
    val main = kotlin.jvm().compilations.getByName("main")
    dependsOn(main.compileTaskProvider)
    classpath = main.output.allOutputs + main.runtimeDependencyFiles
    mainClass.set("codetrail.app.sound.SoundRenderKt")
    jvmArgs("-Djava.awt.headless=true")
    args = listOf(layout.buildDirectory.dir("sounds").get().asFile.path)
}
