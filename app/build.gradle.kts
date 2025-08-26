import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.io.FileInputStream
import java.net.URI
import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
}

val signingProp = file(project.rootProject.file("signing.properties"))

android {
    signingConfigs {
        if (signingProp.exists()) {
            val properties = Properties().apply {
                load(FileInputStream(signingProp))
            }
            create("key") {
                storeFile = rootProject.file(properties.getProperty("keystore.path"))
                storePassword = properties.getProperty("keystore.pwd")
                keyAlias = properties.getProperty("keystore.alias")
                keyPassword = properties.getProperty("keystore.alias_pwd")
            }
        }
    }

    namespace = AppConfiguration.appID
    compileSdk = AppConfiguration.compileSdk

    defaultConfig {
        applicationId = AppConfiguration.appID
        minSdk = AppConfiguration.minSdk
        targetSdk = AppConfiguration.targerSdk
        versionCode = AppConfiguration.versionCode
        versionName = AppConfiguration.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                abiFilters.addAll(listOf("arm64-v8a"))
            }
            if (signingProp.exists()) signingConfig = signingConfigs.getByName("key")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
    }

    applicationVariants.configureEach {
        val variant = this
        outputs.configureEach {
            (this as ApkVariantOutputImpl).apply {
                //@formatter:off
                outputFileName = "GlyphRecorder_${AppConfiguration.versionCode}_${AppConfiguration.versionName.replace(" ","_")}_${variant.buildType.name}.apk"
                //@formatter:on
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":scrcpy-server"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.ktor.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.graphics.core)
    implementation(libs.androidx.graphics.path)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.compose.floating.window)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logging)
    implementation(libs.opencv)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.slf4j.android.mvysny)
    implementation(libs.slf4j.api)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// download glyph data from chibatching/glyph-predictor-data
tasks.register("downloadGlyphData") {
    val assetsDir = file("src/main/assets")
    val glyphCsvFile = file("$assetsDir/glyph.csv")
    val glyphCsvLineFile = file("$assetsDir/glyph_line.csv")
    if (!assetsDir.exists()) assetsDir.mkdirs()

    val downloadFile: (File, String) -> Unit = { file, resourceUrl ->
        if (!file.exists()) {
            URI(resourceUrl).toURL().openStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
                println("Download complete: ${file.absolutePath}")
            }
        }
    }

    downloadFile(glyphCsvFile, AppConfiguration.glyphCsvUrl)
    downloadFile(glyphCsvLineFile, AppConfiguration.glyphCsvLineUrl)
}

tasks.named("preBuild").configure {
    dependsOn("downloadGlyphData")
}