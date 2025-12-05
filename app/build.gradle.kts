plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

android {
    namespace = "com.ngerasimov.rtkar"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.ngerasimov.rtkar"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // BuildConfig fields
        buildConfigField("String", "API_BASE_URL", "\"https://household.key.rt.ru/\"")
        buildConfigField("String", "API_REFERER_URL", "\"https://key.rt.ru/\"")
        buildConfigField("String", "LOGIN_URL", "\"https://key.rt.ru/main/pwa/dashboard\"")
    }

    buildTypes {
        debug {
            // Don't apply ProGuard in debug to avoid stripping classes
            // ProGuard rules are only needed for release builds
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

tasks.withType<Test> {
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
    // Ensure Hilt generated classes are included
    includeCompileClasspath = false
}

// Note: Hilt aggregate deps task has compatibility issues with Kotlin 2.0
// The task is disabled but Hilt should still work
tasks.configureEach {
    if (name.startsWith("hiltAggregateDeps")) {
        enabled = false
    }
}

// Rename APK to rtkar.apk for release builds
android.applicationVariants.all {
    val variant = this
    if (variant.buildType.name == "release") {
        variant.outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "rtkar.apk"
        }
    }
}

// Copy scripts to output directory for release builds
afterEvaluate {
    android.applicationVariants.all {
        val variant = this
        if (variant.buildType.name == "release") {
            // Capture file paths at configuration time to avoid Task.project deprecation
            val buildDirPath = layout.buildDirectory.get().asFile.absolutePath
            val rootDirPath = rootProject.layout.projectDirectory.asFile.absolutePath
            val releaseOutputDirPath = "$buildDirPath/outputs/apk/release"
            val scriptsDirPath = "$rootDirPath/scripts"
            
            val copyScriptsTask = tasks.register("copyScriptsToOutput${variant.name.replaceFirstChar { it.uppercaseChar() }}") {
                doLast {
                    val releaseOutputDir = File(releaseOutputDirPath)
                    val scriptsDir = File(scriptsDirPath)
                    
                    if (!releaseOutputDir.exists()) {
                        releaseOutputDir.mkdirs()
                    }
                    
                    if (scriptsDir.exists()) {
                        // Copy files using Java I/O to avoid Task.project access
                        val filesToCopy = listOf("adb", "install_on_device.sh")
                        filesToCopy.forEach { fileName ->
                            val sourceFile = File(scriptsDir, fileName)
                            if (sourceFile.exists()) {
                                val destFile = File(releaseOutputDir, fileName)
                                sourceFile.copyTo(destFile, overwrite = true)
                                destFile.setExecutable(true)
                            }
                        }
                        println("Copied scripts to ${releaseOutputDir.absolutePath}")
                    } else {
                        println("Warning: Scripts directory not found at ${scriptsDir.absolutePath}")
                    }
                }
            }
            variant.assembleProvider.get().finalizedBy(copyScriptsTask)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.fragment)
    
    testImplementation(libs.junit)
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptTest("com.google.dagger:hilt-compiler:2.52")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.52")
}