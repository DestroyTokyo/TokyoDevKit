pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven("https://www.jetbrains.com/intellij-repository/releases")
		maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
	}
	plugins {
		id("org.jetbrains.intellij.platform") version "2.2.0"
		kotlin("jvm") version "2.2.20"
	}
}

plugins {
	id("org.jetbrains.intellij.platform.settings") version "2.2.0"
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "CherryModKit"
