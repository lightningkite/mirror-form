import com.lightningkite.konvenience.gradle.*
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import java.util.*

plugins {
    kotlin("multiplatform")
    `maven-publish`
    id("com.android.library") //version "3.3.1"
}

buildscript {
    repositories {
        mavenLocal()
        maven("https://dl.bintray.com/lightningkite/com.lightningkite.krosslin")
    }
    dependencies {
        classpath("com.lightningkite:konvenience:+")
    }
}
apply(plugin = "com.lightningkite.konvenience")

repositories {
    mavenLocal()
    google()
    jcenter()
    mavenCentral()
    maven("https://dl.bintray.com/lightningkite/com.lightningkite.krosslin")
    maven("https://kotlin.bintray.com/kotlinx")
}

val versions = Properties().apply {
    load(project.file("versions.properties").inputStream())
}

group = "com.lightningkite"
version = versions.getProperty(project.name)

android {
    project.ext.set("android.useAndroidX", true)
    project.ext.set("android.enableJetifier", true)

    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(28)
//        applicationId("com.lightningkite.koolui")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFile(getDefaultProguardFile("proguard-android-optimize.txt"))
            proguardFile("proguard-rules.pro")
        }
    }
}


kotlin {
    val tryTargets = KTarget.run {
        setOf(
                android,
                javafx,
                jvmVirtual,
                js
        )
    }
    sources(tryTargets = tryTargets) {
        main {
            implementationSet(standardLibrary)
            implementationSet(coroutines(versions.getProperty("kotlinx_coroutines")))
            apiSet(projectOrMavenDashPlatform("com.lightningkite", "kommon", versions.getProperty("kommon"), groupings = KTargetPredicates.binary))
            apiSet(projectOrMavenDashPlatform("com.lightningkite", "lokalize", versions.getProperty("lokalize"), groupings = KTargetPredicates.binary))
            apiSet(projectOrMavenDashPlatform("com.lightningkite", "reacktive", versions.getProperty("reacktive"), groupings = KTargetPredicates.binary))
            apiSet(projectOrMavenDashPlatform("com.lightningkite", "recktangle", versions.getProperty("recktangle"), groupings = KTargetPredicates.binary))
            apiSet(projectOrMavenDashPlatform("com.lightningkite", "mirror-runtime", versions.getProperty("mirror"), groupings = KTargetPredicates.binary))
            apiSet(projectOrMavenDashPlatform("com.lightningkite", "mirror-archive-api", versions.getProperty("mirror"), groupings = KTargetPredicates.binary))
            apiSet(projectOrMavenDashPlatform("com.lightningkite", "koolui", versions.getProperty("koolui"), groupings = KTargetPredicates.ui))
        }
        test {
            implementationSet(testing)
            implementationSet(testingAnnotations)
        }
    }
}

publishing {
    doNotPublishMetadata()
    repositories {
        bintray(
                project = project,
                organization = "lightningkite",
                repository = "com.lightningkite.krosslin"
        )
    }

    appendToPoms {
        github("lightningkite", project.name)
        licenseMIT()
        developers {
            developer {
                id.set("UnknownJoe796")
                name.set("Joseph Ivie")
                email.set("joseph@lightningkite.com")
                timezone.set("America/Denver")
                roles.set(listOf("architect", "developer"))
                organization.set("Lightning Kite")
                organizationUrl.set("http://www.lightningkite.com")
            }
        }
    }
}
