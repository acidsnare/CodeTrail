import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()

    sourceSets {
        jvmMain {
            // Art lives at the repo root so the future web module can share it.
            // Only character SVGs and the runtime window icon are bundled; installer icons stay out of the jar.
            resources.srcDir(rootProject.file("assets"))
            resources.exclude("icon/*.icns", "icon/*.ico", "icon/*.svg", "icon/*_1024.png")
            dependencies {
                implementation(project(":core"))
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "codetrail.desktop.res"
    generateResClass = always
}

compose.desktop {
    application {
        mainClass = "codetrail.desktop.MainKt"
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        // -Xdock:name fixes the "java" dock tooltip when running from Gradle. It is a macOS-only
        // flag: on Windows or Linux an unknown -X option stops the JVM from starting at all.
        if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) jvmArgs("-Xdock:name=CodeTrail")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "CodeTrail"
            packageVersion = "1.0.0"
            description = "Program your hero's path"
            vendor = "CodeTrail"
            macOS {
                iconFile.set(rootProject.file("assets/icon/codetrail.icns"))
                bundleID = "dev.codetrail.app"
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
    mainClass.set("codetrail.desktop.SnapshotKt")
    jvmArgs("--enable-native-access=ALL-UNNAMED", "-Djava.awt.headless=true")
    args = listOf(
        providers.gradleProperty("out").getOrElse("build/snapshot.png"),
        providers.gradleProperty("tier").getOrElse("3"),
        providers.gradleProperty("seed").getOrElse("7"),
        if (providers.gradleProperty("solve").isPresent) "solve" else if (providers.gradleProperty("fail").isPresent) "fail" else if (providers.gradleProperty("hint").isPresent) "hint" else "edit",
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
    mainClass.set("codetrail.desktop.IconRenderKt")
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
    mainClass.set("codetrail.desktop.sound.SoundRenderKt")
    jvmArgs("-Djava.awt.headless=true")
    args = listOf(layout.buildDirectory.dir("sounds").get().asFile.path)
}
