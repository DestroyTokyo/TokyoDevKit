pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven("https://www.jetbrains.com/intellij-repository/releases")
		maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
	}
	plugins {
		id("org.jetbrains.intellij.platform") version "2.2.0"
	}
}

plugins {
	id("org.jetbrains.intellij.platform.settings") version "2.2.0"
}

rootProject.name = "Mushroom"
