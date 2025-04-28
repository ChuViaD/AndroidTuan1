plugins {
    alias(libs.plugins.android.application) apply false
    id("com.android.library") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false  // Thêm plugin Kotlin
}

/*dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
}*/
