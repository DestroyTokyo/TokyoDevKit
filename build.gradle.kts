plugins {
	id("java")
	id("org.jetbrains.intellij.platform")
	kotlin("jvm")
}

val jsonVersion: String by project
val modKitVersion: String by project

group = "delta.cion.cherry.modKit"
version = modKitVersion

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	intellijPlatform {
		defaultRepositories()
	}
}

dependencies {
	intellijPlatform {
		intellijIdeaCommunity("2024.1.7")
		bundledPlugin("com.intellij.java")
		bundledPlugin("org.jetbrains.plugins.gradle")
	}
	implementation(kotlin("stdlib-jdk8"))
	implementation("org.json:json:${jsonVersion}")
}

intellijPlatform {
	pluginConfiguration {
		ideaVersion {
			sinceBuild = "241"
		}
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
}

kotlin {
	jvmToolchain(21)
}
