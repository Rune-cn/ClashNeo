@file:Suppress("UNUSED_VARIABLE")

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationBaseFlavor
import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.BaseFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryBaseFlavor
import com.android.build.api.dsl.ProductFlavor
import org.gradle.api.NamedDomainObjectContainer
import java.net.URL
import java.util.*

buildscript {
    repositories {
        mavenCentral()
        google()
        maven("https://raw.githubusercontent.com/MetaCubeX/maven-backup/main/releases")
    }
    dependencies {
        classpath(libs.build.android)
        classpath(libs.build.kotlin.common)
        classpath(libs.build.kotlin.serialization)
        classpath(libs.build.ksp)
        classpath(libs.build.golang)
        // Compose compiler plugin; its version must always track the Kotlin version above.
        classpath(libs.build.compose.compiler)
    }
}

subprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://raw.githubusercontent.com/MetaCubeX/maven-backup/main/releases")
        // miuix is published to GitHub Packages, which never allows anonymous access.
        // Credentials live in ~/.gradle/gradle.properties (`gpr.user` / `gpr.key`) or in the
        // GITHUB_ACTOR / GITHUB_TOKEN environment variables; they are never committed.
        // The content filter keeps every other dependency from being requested here, so a
        // missing token cannot slow down (or fail) unrelated resolution.
        val gprUser = providers.gradleProperty("gpr.user")
            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
        val gprKey = providers.gradleProperty("gpr.key")
            .orElse(providers.environmentVariable("GITHUB_TOKEN"))

        maven {
            name = "GitHubPackagesMiuix"
            url = uri("https://maven.pkg.github.com/compose-miuix-ui/miuix")
            content { includeGroup("top.yukonga.miuix.kmp") }

            if (gprUser.isPresent && gprKey.isPresent) {
                credentials {
                    username = gprUser.get()
                    password = gprKey.get()
                }
            }
        }
    }

    val isApp = name == "app"

    apply(plugin = if (isApp) "com.android.application" else "com.android.library")

    fun queryConfigProperty(key: String): Any? {
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        } else {
            return null
        }
        return localProperties.getProperty(key)
    }

    // AGP 9 removed the legacy `BaseExtension` / `AppExtension` types in favour of the
    // `com.android.build.api.dsl` interfaces. `CommonExtension` is non-generic in AGP 9 and
    // covers app and library modules alike, so one configuration block keeps serving both.
    extensions.configure<CommonExtension> {
        buildFeatures.buildConfig = true
        // `resValue(...)` in `defaultConfig` requires the resValues build feature to be on;
        // AGP 9 no longer enables it implicitly.
        buildFeatures.resValues = true

        // Containers of *out*-projected AGP DSL types cannot drive Kotlin lambda receivers, so
        // each is bound once to a concretely parameterised view. The casts are safe: the
        // containers are populated by AGP with these exact runtime types, and the projection is
        // only a declaration-site restriction. `@Suppress` keeps the file warning-clean because
        // the project treats warnings as errors.
        @Suppress("UNCHECKED_CAST")
        val typedSourceSets = sourceSets as NamedDomainObjectContainer<AndroidSourceSet>
        @Suppress("UNCHECKED_CAST")
        val typedSigningConfigs = signingConfigs as NamedDomainObjectContainer<ApkSigningConfig>
        @Suppress("UNCHECKED_CAST")
        val typedBuildTypes = buildTypes as NamedDomainObjectContainer<ApplicationBuildType>

        // AGP 9's `CommonExtension` exposes `defaultConfig` only as the `DefaultConfig`
        // supertype, which carries neither `applicationId`/`targetSdk`/`versionName` (those live
        // on `ApplicationBaseFlavor`) nor a usable lambda receiver. The shared portion is
        // therefore written against `BaseFlavor`, and the app-only fields behind an `isApp`
        // branch on the concrete subtype. Same values as before, just typed correctly.
        val typedDefaultConfig: BaseFlavor = defaultConfig
        typedDefaultConfig.apply {
            if (isApp) {
                val customApplicationId = queryConfigProperty("custom.application.id") as? String?
                (this as ApplicationBaseFlavor).applicationId =
                    customApplicationId.takeIf { it?.isNotBlank() == true } ?: "io.github.neo.clash"
            }

            project.name.let { name ->
                namespace = if (name == "app") "io.github.neo.clash"
                else "io.github.neo.clash.$name"
            }

            // 24 is the lowest value the miuix Compose foundation supports (its
            // miuix-ui/miuix-material artifacts declare minSdk 24). The glass/blur artifacts
            // declare 33 and are opted into via `tools:overrideLibrary`, guarded at runtime by
            // `isOs4PlatformSupported` (SDK >= 33 && render effect available).
            minSdk = 24

            ndk {
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            }

            externalNativeBuild {
                cmake {
                    abiFilters("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                }
            }

            if (isApp) {
                val appFlavor = this as ApplicationBaseFlavor
                appFlavor.targetSdk = 35

                appFlavor.versionName = "2.11.37"
                appFlavor.versionCode = 211037

                resValue("string", "release_name", "v2.11.37")
                resValue("integer", "release_code", "211037")
            } else {
                (this as com.android.build.api.dsl.LibraryBaseFlavor)
                    .consumerProguardFiles("consumer-rules.pro")
            }
        }

        ndkVersion = "29.0.14206865"

        // miuix AARs declare `minCompileSdk=37.2`, so the whole project compiles against that
        // minor while `targetSdk` stays at 35 to avoid changing the XML UI's runtime behaviour.
        // `compileSdk` is the AGP 9 spelling; `compileSdkVersion(Int)` is deprecated for 10.0.
        // The trailing `.2` matters: 37.0 is rejected by the AAR metadata check.
        compileSdk = 37
        compileSdkMinor = 2

        if (isApp) {
            // AGP 9 replaced `packagingOptions { resources { } }` with `packaging.resources`.
            packaging.resources.excludes.add("DebugProbesKt.bin")
        }

        // AGP 9 declares `productFlavors` / `sourceSets` / `buildTypes` / `signingConfigs` as
        // containers of *out*-projected DSL types, so Kotlin cannot infer a lambda receiver for
        // `create("x") { }` or `getByName("x") { }`. Binding each container to a concretely
        // parameterised local restores the readable `create`/`getByName` form.
        @Suppress("UNCHECKED_CAST")
        val typedProductFlavors =
            productFlavors as NamedDomainObjectContainer<ApplicationProductFlavor>

        // `flavorDimensions` is a mutable `List<String>` on `CommonExtension` in AGP 9 (it used to
        // be a vararg function), so the dimension is appended rather than invoked.
        flavorDimensions += "feature"

        typedProductFlavors.apply {
            val removeSuffix = (queryConfigProperty("remove.suffix") as? String)?.toBoolean() == true

            fun ApplicationProductFlavor.applyCommon() {
                dimension = flavorDimensions[0]
                buildConfigField("boolean", "PREMIUM", "Boolean.parseBoolean(\"false\")")
            }

            create("alpha") {
                isDefault = true
                applyCommon()
                if (!removeSuffix) {
                    versionNameSuffix = ".Alpha"
                }

                resValue("string", "launch_name", "@string/launch_name_alpha")
                resValue("string", "application_name", "@string/application_name_alpha")

                if (isApp && !removeSuffix) {
                    (this as ApplicationProductFlavor).applicationIdSuffix = ".alpha"
                }
            }

            create("meta") {
                applyCommon()
                if (!removeSuffix) {
                    versionNameSuffix = ".Meta"
                }

                resValue("string", "launch_name", "@string/launch_name_meta")
                resValue("string", "application_name", "@string/application_name_meta")

                if (isApp && !removeSuffix) {
                    (this as ApplicationProductFlavor).applicationIdSuffix = ".meta"
                }
            }
        }

        // Both flavours reuse the same FOSS sources, as before.
        listOf("meta", "alpha").forEach { flavor ->
            typedSourceSets.getByName(flavor).java.srcDirs("src/foss/java")
        }

        signingConfigs {
            val keystore = rootProject.file("signing.properties")
            if (keystore.exists()) {
                typedSigningConfigs.create("release") {
                    val prop = Properties().apply {
                        keystore.inputStream().use(this::load)
                    }

                    storeFile = rootProject.file("release.keystore")
                    storePassword = prop.getProperty("keystore.password")!!
                    keyAlias = prop.getProperty("key.alias")!!
                    keyPassword = prop.getProperty("key.password")!!
                }
            }
        }

        typedBuildTypes.apply {
            named("release", ApplicationBuildType::class) {
                isMinifyEnabled = isApp
                isShrinkResources = isApp
                signingConfig = typedSigningConfigs.findByName("release") ?: typedSigningConfigs["debug"]
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
            named("debug") {
                versionNameSuffix = ".debug"
            }
        }

        // `dataBinding` lives on `CommonExtension`, not on `BuildFeatures` (only `viewBinding`
        // and `compose` are build features), so it is configured as a sibling.
        dataBinding.isEnabled = name != "hideapi"

        buildFeatures.apply {
            // Compose is enabled for the app module only: the Compose UI entry point lives
            // there, next to the service binding it reuses.
            if (isApp) {
                compose = true
            }
        }

        if (isApp) {
            // `splits` only exists on the application DSL, so narrow from the common supertype.
            this as ApplicationExtension

            splits {
                abi {
                    isEnable = true
                    isUniversalApk = true
                    reset()
                    include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
                }
            }
        }

        // Same receiver-resolution reason as the containers above: `compileOptions` is a property
        // whose type is an AGP interface, so it is configured directly instead of via a lambda.
        compileOptions.sourceCompatibility = JavaVersion.VERSION_21
        compileOptions.targetCompatibility = JavaVersion.VERSION_21
    }
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL

    doLast {
        val sha256 = URL("$distributionUrl.sha256").openStream()
            .use { it.reader().readText().trim() }

        file("gradle/wrapper/gradle-wrapper.properties")
            .appendText("distributionSha256Sum=$sha256")
    }
}