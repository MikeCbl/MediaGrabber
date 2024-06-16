import com.android.build.api.variant.FilterConfiguration

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Map for the version code that gives each ABI a value.
val abiCodes = mapOf("armeabi-v7a" to 1, "x86" to 2, "x86_64" to 3, "arm64-v8a" to 4)

////val properties = Properties()
//val versionMajor = 1
//val versionMinor = 6
//val versionPatch = 8
//val versionBuild = 4 // bump for dogfood builds, public betas, etc.
//var versionExt = ""
//
//if (versionBuild > 0){
//    versionExt = ".${versionBuild}-beta"
//}

androidComponents {
    onVariants { variant ->

        // Assigns a different version code for each output APK
        // other than the universal APK.
        variant.outputs.forEach { output ->
            val name = output.filters.find { it.filterType == FilterConfiguration.FilterType.ABI }?.identifier

            // Stores the value of abiCodes that is associated with the ABI for this variant.
            val baseAbiCode = abiCodes[name]
            // Because abiCodes.get() returns null for ABIs that are not mapped by ext.abiCodes,
            // the following code doesn't override the version code for universal APKs.
            // However, because you want universal APKs to have the lowest version code,
            // this outcome is desirable.
            if (baseAbiCode != null) {
                // Assigns the new version code to output.versionCode, which changes the version code
                // for only the output APK, not for the variant itself.
                output.versionCode.set(baseAbiCode * 1000 + (output.versionCode.get() ?: 0))
            }
        }
    }
}

android {
    namespace = "com.example.mediagrabber"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mediagrabber"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //dodane ndk
        ndk {
            abiFilters.addAll(arrayOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a"))
        }
//        roznica 1 mb :/
//        ndk {
//            abiFilters.add("arm64-v8a")
//        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    splits {
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = true
            // By default all ABIs are included, so use reset() and include to specify that you only
            // want APKs for x86 and x86_64.
            reset()
            // Specifies a list of ABIs for Gradle to create APKs for.
            include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            // Specifies that you don't want to also generate a universal APK that includes all ABIs.
            isUniversalApk = true
        }
    }

    //for binding views etc
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // Dependencies on local binaries
    implementation(fileTree(mapOf("dir" to "libs", "include" to  listOf("*.jar"))))

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.0")
    implementation("com.devbrackets.android:exomedia:5.0.0")
    //dexter for permissions
    implementation("com.karumi:dexter:6.2.3")
    //swipe etc
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    // youtubedl-android
    implementation("com.github.yausername.youtubedl-android:library:-SNAPSHOT")
    implementation("com.github.yausername.youtubedl-android:ffmpeg:-SNAPSHOT")
    implementation("com.github.yausername.youtubedl-android:aria2c:-SNAPSHOT")
    // video
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    //QR
    implementation("com.google.zxing:core:3.4.1")
    implementation("com.journeyapps:zxing-android-embedded:4.2.0")


}