plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
        }
    }
}

tasks.register<JavaExec>("demo") {
    group = "application"
    description = "Print sample levels for every tier and stress-test the generator."
    val jvmTarget = kotlin.jvm()
    val main = jvmTarget.compilations.getByName("main")
    dependsOn(main.compileTaskProvider)
    classpath = main.output.allOutputs + main.runtimeDependencyFiles
    mainClass.set("codetrail.demo.MainKt")
    args = providers.gradleProperty("seed").map { listOf(it) }.getOrElse(emptyList())
}
