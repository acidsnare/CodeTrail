plugins {
    id("com.android.application")
    kotlin("plugin.compose")
}

// Thin Android shell: the whole game lives in :app (commonMain + androidMain); this module only
// hosts the activity, manifest, launcher icons and the APK packaging.
android {
    namespace = "codetrail.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "dk.codetrail.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 150
        versionName = "1.5.0"
    }
    // Release signing comes from the environment (CI secrets or a local shell). Without it the
    // release build falls back to the debug key, which is fine for sideloading but not for Play.
    val keystorePath = System.getenv("ANDROID_KEYSTORE_FILE")
    val hasReleaseKey = !keystorePath.isNullOrBlank() && file(keystorePath).exists()
    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            // No shrinking: the app is small and Compose resources are looked up by name.
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (hasReleaseKey) "release" else "debug")
        }
    }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":core"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.17.0")
}

// Compose Multiplatform 1.12 does not package compose resources from a
// com.android.kotlin.multiplatform.library module into the APK (its copy task has no output).
// Until it does, take :app's prepared resources and ship them as assets under the path the
// runtime reader expects: assets/composeResources/<Res package>/...
abstract class CopyComposeAssets : DefaultTask() {
    @get:InputDirectory
    abstract val source: DirectoryProperty

    @get:Input
    abstract val resPackage: Property<String>

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @TaskAction
    fun run() {
        val dst = output.get().asFile.resolve("composeResources/${resPackage.get()}")
        dst.deleteRecursively()
        source.get().asFile.copyRecursively(dst, overwrite = true)
    }
}

val composeAssets = tasks.register<CopyComposeAssets>("copyComposeResourcesToAssets") {
    dependsOn(":app:prepareComposeResourcesTaskForCommonMain")
    source.set(project(":app").layout.buildDirectory.dir("generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"))
    resPackage.set("codetrail.app.res")
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(composeAssets, CopyComposeAssets::output)
    }
}
