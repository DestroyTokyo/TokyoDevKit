plugins {
	id("java")
	id("org.jetbrains.intellij.platform")
	kotlin("jvm")
}

group = "delta.cion.cherry.modkit"
version = "v0.0.0-dev"

java {
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 21
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
}

intellijPlatform {
	pluginConfiguration {
		ideaVersion {
			sinceBuild = "241"
		}
	}
}
kotlin {
	jvmToolchain(21)
}
