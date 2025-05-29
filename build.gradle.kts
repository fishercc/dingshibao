



android {
    namespace = "com.dingshibao.dko"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dingshibao.dko"
        minSdk = 26
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        // 配置开启buildConfig构建特性
        buildConfig = true
        compose = true
    }

    // 添加打包配置
    packaging {
        resources {
            excludes += listOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true  // 启用混淆
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 用于标识release环境条件编译
            buildConfigField("Boolean", "DEBUG", "false")
        }
        debug {

            // 用于标识debug环境条件编译
            buildConfigField("Boolean", "DEBUG", "true")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {


    implementation("com.github.MuntashirAkon:sun-security-android:1.1")
    implementation("org.conscrypt:conscrypt-android:2.5.2")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation ("org.greenrobot:eventbus:3.3.1")
    implementation ("com.github.getActivity:XXPermissions:20.0")
    implementation ("com.github.getActivity:Toaster:12.6")
    implementation ("com.google.code.gson:gson:2.11.0")
    implementation ("com.github.guolindev:LitePal:8ad8322cc6")
    implementation ("com.github.xkzhangsan:xk-time:3.2.4")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.viewmodel.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



}



// 确保 app 模块依赖 server 模块的编译任务
tasks.named("preBuild").configure {
    dependsOn(":server:assembleDebug", "copyServerApkToAssets")
}
