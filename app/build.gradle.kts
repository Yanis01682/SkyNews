
plugins {
    alias(libs.plugins.android.application)
}

val localProperties = java.util.Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.java.zhangzhiyuan"
    compileSdk = 36



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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    defaultConfig {
        applicationId = "com.java.zhangzhiyuan"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        buildConfigField("String", "ZHIPU_API_KEY", "\"${localProperties.getProperty("ZHIPU_API_KEY", "")}\"")
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    dependencies {
        implementation("com.google.android.material:material:1.5.0")
        // 安卓基础库 (保留)
        implementation("androidx.core:core-ktx:1.9.0")
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("com.google.android.material:material:1.11.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
        implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
        implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
        implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
        implementation(libs.swiperefreshlayout)
        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

        // Room数据库
        val room_version = "2.6.1"
        implementation("androidx.room:room-runtime:$room_version")
        annotationProcessor("androidx.room:room-compiler:$room_version")

        // 网络请求和图片加载
        implementation("com.squareup.retrofit2:retrofit:2.9.0")
        implementation("com.squareup.retrofit2:converter-gson:2.9.0")
        implementation("com.google.code.gson:gson:2.9.0")
        implementation("com.github.bumptech.glide:glide:4.12.0")
        annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")


        // ExoPlayer依赖
        implementation("androidx.media3:media3-exoplayer:1.3.1")
        implementation("androidx.media3:media3-ui:1.3.1")
        // 为ExoPlayer添加OkHttp网络引擎
        implementation("androidx.media3:media3-datasource-okhttp:1.3.1")



        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

        //以支持 RecyclerView 的拖拽和 TabLayout
        implementation("androidx.viewpager2:viewpager2:1.0.0")
        implementation("com.google.android.material:material:1.5.0")

        implementation("androidx.recyclerview:recyclerview:1.2.1")

    }
}
