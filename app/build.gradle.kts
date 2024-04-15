plugins {
    alias(libs.plugins.androidApplication)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.groupproject.music"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.groupproject.music"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("jp.wasabeef:recyclerview-animators:4.0.2")
    implementation("io.github.gautamchibde:audiovisualizer:2.2.5")
    implementation("com.github.jgabrielfreitas:BlurImageView:1.0.1")
    implementation("com.google.android.exoplayer:exoplayer:2.17.1")
    implementation("androidx.palette:palette:1.0.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
}