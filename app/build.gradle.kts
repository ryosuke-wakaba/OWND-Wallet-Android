import com.google.protobuf.gradle.proto

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}
android {
    namespace = "com.ownd_project.tw2023_wallet_android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ownd_project.tw2023_wallet_android"
        minSdk = 33
        targetSdk = 33
        versionCode = 24
        versionName = "0.1.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            resValue("string", "MYNA_TARGET_URL", "https://ownd-project.com:8443")
            resValue(
                "string",
                "TERM_OF_USE_URL",
                "https://www.ownd-project.com/wallet/tos/index.html"
            )
            resValue(
                "string",
                "PRIVACY_POLICY_URL",
                "https://www.ownd-project.com/wallet/privacy/index.html"
            )
            resValue("string", "SUPPORT_URL", "https://ownd-project.zendesk.com/hc/ja/requests/new")
            resValue(
                "string",
                "LICENCE_URL",
                "https://www.ownd-project.com/license/license-notice.txt"
            )
            resValue("string", "MESSENGER_URL", "https://app.element.io/")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            resValue("string", "MYNA_TARGET_URL", "https://ownd-project.com:8443")
            resValue(
                "string",
                "TERM_OF_USE_URL",
                "https://www.ownd-project.com/wallet/tos/index.html"
            )
            resValue(
                "string",
                "PRIVACY_POLICY_URL",
                "https://www.ownd-project.com/wallet/privacy/index.html"
            )
            resValue("string", "SUPPORT_URL", "https://bunsin.zendesk.com/hc/ja/requests/new")
            resValue(
                "string",
                "LICENCE_URL",
                "https://www.ownd-project.com/license/license-notice.txt"
            )
            resValue("string", "MESSENGER_URL", "https://app.element.io/")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
        compose = true
    }
    sourceSets {
        getByName("main") {
            java.srcDirs("generated/main/grpc", "generated/main/java")
            proto { srcDir("src/main/proto") }
        }
    }
}

configurations {
    all {
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
    }
}

dependencies {
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.activity:activity:1.9.3")
    androidTestImplementation(project(":app"))
    androidTestImplementation(project(":app"))
    // compose setting start
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.6")
    // compose setting end

    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.browser:browser:1.5.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.datastore:datastore:1.0.0")
    implementation("androidx.datastore:datastore-core:1.0.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.google.protobuf:protobuf-javalite:3.24.4")
    implementation("com.growingio.android:okhttp3:3.5.1")
    implementation("io.arrow-kt:arrow-core:1.2.1")
    implementation("io.arrow-kt:arrow-fx-coroutines:1.2.1")
    implementation("org.bitbucket.b_c:jose4j:0.9.3")
    implementation("org.bitcoinj:bitcoinj-core:0.16")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.web3j:core:3.3.1-android")
    implementation("com.github.corouteam:GlideToVectorYou:2.0.0")
    implementation("com.github.lelloman:android-identicons:v11")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.auth0:jwks-rsa:0.22.1") {
        exclude(group = "com.google.guava", module = "guava")
    }
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("com.luhuiguo.bouncycastle:bcpkix-jdk15on:1.70.GM") {
        exclude(group = "com.luhuiguo.bouncycastle", module = "bcprov-jdk15on")
    }
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.authlete:sd-jwt:1.4")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.mockito:mockito-android:5.6.0")
    testImplementation("org.wiremock:wiremock:3.2.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
    debugImplementation("androidx.fragment:fragment-testing:1.8.5")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.4"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create(
                    "java"
                ) {
                    option("lite")
                }
            }
        }
    }
}