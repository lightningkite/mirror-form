pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        jcenter()
        mavenLocal()
        maven("https://dl.bintray.com/lightningkite/com.lightningkite.krosslin")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.namespace == "com.android") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
}

enableFeaturePreview("GRADLE_METADATA")
