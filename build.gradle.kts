import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("org.jetbrains.kotlin.jvm") version "2.3.21"
	id("org.jetbrains.intellij.platform") version "2.18.1"
}

val jsonVersion: String by project
val modKitVersion: String by project

group = "delta.cion.tokyo.modKit"
version = modKitVersion

kotlin {
	jvmToolchain(21)
	compilerOptions {
		jvmTarget = JvmTarget.JVM_21
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

repositories {
	mavenCentral()
	gradlePluginPortal()
	intellijPlatform {
		defaultRepositories()
	}
}

dependencies {
	implementation("org.json:json:${jsonVersion}")
	intellijPlatform {
		intellijIdea("2025.2.6.2")
		bundledPlugin("com.intellij.java")
		bundledPlugin("com.intellij.gradle")
	}
}

// https://www.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIU/2025.2.6.2/ideaIU-2025.2.6.2.pom

intellijPlatform {
	pluginConfiguration {
		ideaVersion {
			sinceBuild = "252"
			untilBuild = "999"
		}
	}
}
