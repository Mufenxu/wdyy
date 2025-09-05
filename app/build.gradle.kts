import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // EMAS Serverless 接入信息（Secret 通过 local.properties 注入，避免入库）
        buildConfigField("String", "EMAS_SPACE_ID", "\"mp-1cf30b47-c0fa-4e5a-b674-664e1a24b574\"")
        buildConfigField("String", "EMAS_ENDPOINT", "\"https://api.next.bspapp.com\"")
        val lp = Properties()
        val lpFile = rootProject.file("local.properties")
        if (lpFile.exists()) lpFile.inputStream().use { inputStream -> lp.load(inputStream) }
        val emasSecret = lp.getProperty("EMAS_SECRET", "")
        val emasSecretHeader = lp.getProperty("EMAS_SECRET_HEADER", "X-Space-Secret")
        buildConfigField("String", "EMAS_SECRET", "\"$emasSecret\"")
        buildConfigField("String", "EMAS_SECRET_HEADER", "\"$emasSecretHeader\"")
    }

    signingConfigs {
        create("release") {
            val storeFilePath = project.properties["RELEASE_STORE_FILE"] as String
            storeFile = file(storeFilePath)
            storePassword = project.properties["RELEASE_STORE_PASSWORD"] as String
            keyAlias = project.properties["RELEASE_KEY_ALIAS"] as String
            keyPassword = project.properties["RELEASE_KEY_PASSWORD"] as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Material Icons (version provided by BOM)
    implementation("androidx.compose.material:material-icons-extended")
    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")
    // Aliyun Cloud Push SDK
    implementation("com.aliyun.ams:alicloud-android-push:3.8.2")
    // HTTP 客户端用于调用 EMAS Serverless 云函数
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // 已移除测试与预览依赖以瘦身；如需单元/UI 测试可再添加
}