plugins { id("com.android.application") }

android {
    namespace = "org.mosqueautosilent.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.mosqueautosilent.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.5.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        encoding = "UTF-8"
    }

    sourceSets.getByName("test").java.srcDir("../../tests")
    sourceSets.getByName("main").assets.srcDir("../../data")
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core:1.16.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    testImplementation("junit:junit:4.13.2")
}
