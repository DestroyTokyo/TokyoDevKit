plugins {
	id("java")
	id("org.jetbrains.intellij.platform")
}

group = "delta.cion.mushoom"
version = "0.0.1"

java {
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
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
}

intellijPlatform {
	pluginConfiguration {
		ideaVersion {
			sinceBuild = "241"
		}
	}
}
