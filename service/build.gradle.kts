plugins {
    kotlin("android")
    id("kotlinx-serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":common"))

    ksp(libs.kaidl.compiler)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlin.coroutine)
    implementation(libs.kotlin.serialization.json)
    implementation(libs.androidx.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kaidl.runtime)
    implementation(libs.rikkax.multiprocess)
}

// KSP registers its `generated/ksp/<variant>/kotlin` and `.../java` outputs on the matching
// source sets itself. The manual `afterEvaluate { android { libraryVariants.forEach { ... } } }`
// wiring that used to live here both duplicated that registration and, under AGP 9, forced a cast
// of the decorated source set to the removed `AndroidLibrarySourceSet` type, which threw a
// ClassCastException during configuration. Nothing replaces it: the plugin's own wiring suffices.
