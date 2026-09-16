plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace="com.folbazar.admin"
    compileSdk=35
    defaultConfig { applicationId="com.folbazar.admin"; minSdk=26; targetSdk=35; versionCode=1; versionName="1.0.0" }
    buildFeatures { compose=true; buildConfig=true }
    val lp=java.util.Properties(); val lf=rootProject.file("local.properties"); if(lf.exists()) lf.inputStream().use{lp.load(it)}
    fun prop(n:String)=lp.getProperty(n, "")
    buildTypes {
        debug { buildConfigField("String","SUPABASE_URL","\"${prop("SUPABASE_URL")}\""); buildConfigField("String","SUPABASE_PUBLISHABLE_KEY","\"${prop("SUPABASE_PUBLISHABLE_KEY")}\""); buildConfigField("String","CLOUDINARY_CLOUD_NAME","\"${prop("CLOUDINARY_CLOUD_NAME")}\""); buildConfigField("String","CLOUDINARY_UPLOAD_PRESET","\"${prop("CLOUDINARY_UPLOAD_PRESET")}\"") }
        release { isMinifyEnabled=false; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro"); buildConfigField("String","SUPABASE_URL","\"${System.getenv("SUPABASE_URL") ?: prop("SUPABASE_URL")}\""); buildConfigField("String","SUPABASE_PUBLISHABLE_KEY","\"${System.getenv("SUPABASE_PUBLISHABLE_KEY") ?: prop("SUPABASE_PUBLISHABLE_KEY")}\""); buildConfigField("String","CLOUDINARY_CLOUD_NAME","\"${System.getenv("CLOUDINARY_CLOUD_NAME") ?: prop("CLOUDINARY_CLOUD_NAME")}\""); buildConfigField("String","CLOUDINARY_UPLOAD_PRESET","\"${System.getenv("CLOUDINARY_UPLOAD_PRESET") ?: prop("CLOUDINARY_UPLOAD_PRESET")}\"") }
    }
    compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget="17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.6")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
