import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
    id("signing")
}

val publishedArtifactId = "compose-richtext-core"
val publishRepositoryUrl = providers.environmentVariable("COMPOSE_RICH_EDITOR_PUBLISH_URL")
val publishRepositoryUsername = providers.environmentVariable("COMPOSE_RICH_EDITOR_PUBLISH_USERNAME")
val publishRepositoryPassword = providers.environmentVariable("COMPOSE_RICH_EDITOR_PUBLISH_PASSWORD")
val signingKey = providers.environmentVariable("COMPOSE_RICH_EDITOR_SIGNING_KEY")
val signingPassword = providers.environmentVariable("COMPOSE_RICH_EDITOR_SIGNING_PASSWORD")

kotlin {
    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    androidLibrary {
        namespace = "com.taocent.simple.compose.component.richtext.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

publishing {
    repositories {
        publishRepositoryUrl.orNull?.takeIf { it.isNotBlank() }?.let { repositoryUrl ->
            maven {
                name = "remote"
                url = uri(repositoryUrl)
                credentials {
                    username = publishRepositoryUsername.orNull
                    password = publishRepositoryPassword.orNull
                }
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
        artifactId = if (name == "kotlinMultiplatform") publishedArtifactId else "$publishedArtifactId-$name"
        pom {
            name.set("ComposeRichEditor Core")
            description.set("Core state, formatting, paragraph model, emoji, paste, serialization, platform adapters, and shared internals for ComposeRichEditor.")
            url.set("https://github.com/Taocent/ComposeRichEditor")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set("Taocent")
                    name.set("Taocent")
                }
            }
            scm {
                url.set("https://github.com/Taocent/ComposeRichEditor")
                connection.set("scm:git:https://github.com/Taocent/ComposeRichEditor.git")
                developerConnection.set("scm:git:ssh://git@github.com/Taocent/ComposeRichEditor.git")
            }
        }
    }
}

configure<SigningExtension> {
    val key = signingKey.orNull
    val password = signingPassword.orNull
    if (!key.isNullOrBlank() && !password.isNullOrBlank()) {
        useInMemoryPgpKeys(key, password)
        sign(publishing.publications)
    }
}
