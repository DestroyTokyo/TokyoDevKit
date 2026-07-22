plugins {
	id("java")
	id("org.jetbrains.intellij.platform")
	id("com.gradleup.shadow") version "8.3.0"
	kotlin("jvm")
}

val jsonVersion: String by project
val modKitVersion: String by project

group = "delta.cion.tokyo.modKit"
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
	implementation("org.json:json:$jsonVersion")
}

intellijPlatform {
	pluginConfiguration {
		ideaVersion {
			sinceBuild = "241"
			untilBuild = "999"
		}
	}
}

tasks {
	jar {
		dependsOn(shadowJar)
		from(zipTree(shadowJar.get().archiveFile))
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}

	withType<JavaCompile>().configureEach {
		options.release = 21
	}

	shadowJar {
		mergeServiceFiles()
		archiveClassifier.set("")
		dependencies {
			include(dependency("org.json:json:.*"))
		}
	}

	buildPlugin {
		dependsOn(shadowJar)
	}

	build {
		dependsOn(shadowJar)
	}

	named("prepareTestSandbox") {
		dependsOn(shadowJar)
	}
}

kotlin {
	jvmToolchain(21)
}
