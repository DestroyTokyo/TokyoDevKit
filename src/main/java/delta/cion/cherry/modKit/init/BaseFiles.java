package delta.cion.cherry.modKit.init;

public class BaseFiles {

	/**
	 * No needed values.
	 */
	private static final String GIT_IGNORE =
		"""
		# ==================== #
		# By: Lmao stupid Cion #
		# ==================== #

		# <= Main =>
		.env
		*.iml
		*.iws
		.zed
		.idea

		# <= Sometimes it appears =>
		./src/
		test/

		# <= Gradle =>
		.gradle
		build
		gradlew
		gradlew.bat

		# <= Maven =>
		target/

		#   >> Contain
		!.gitattributes
		gradle/wrapper/gradle-wrapper.jar

		# <= Server =>
		server.properties
		plugins/
		logs/
		crash_logs/
		""";

	public static String getGitIgnore() {
		return GIT_IGNORE;
	}

	/**
	 * Needed values:
	 * gradle version
	 */
	private static final String WRAPPER_PROPERTIES =
		"""
		distributionBase=GRADLE_USER_HOME
		distributionPath=wrapper/dists
		distributionUrl=https\\://services.gradle.org/distributions/gradle-%s-bin.zip
		zipStoreBase=GRADLE_USER_HOME
		zipStorePath=wrapper/dists
		""";

	public static String getWrapperProperties(String gradleVersion) {
		return WRAPPER_PROPERTIES.formatted(gradleVersion);
	}

	/**
	 * Needed values:
	 * project name
	 */
	private static final String SETTINGS_GRADLE =
		"""
		rootProject.name = "%s"
		""";

	public static String getSettingsGradle(String projectName) {
		return WRAPPER_PROPERTIES.formatted(projectName);
	}

	/**
	 * Needed values:
	 * shadow version,
	 * project group,
	 * project version,
	 * java version,
	 * cherry_api version
	 */
	private static final String BUILD_GRADLE =
		"""
			plugins {
				id("java")
				id("com.gradleup.shadow") version("%s")
			}

			group = %s
			version = %s

			java {
				toolchain {
					languageVersion = JavaLanguageVersion.of(%s)
				}
			}

			repositories {
				mavenLocal()
			    mavenCentral()
			}

			dependencies {
				compileOnly("delta.cion.cherry.api:cherry_api:%s")
			}

			tasks {
				withType<JavaCompile> {
					options.encoding = "UTF-8"
				}

				build {
					dependsOn(shadowJar)
				}

				shadowJar {
					mergeServiceFiles()
					archiveClassifier.set("")
				}
			}
		""";

	public static String getBuildGradle(String shadowVersion, String projectGroup, String projectVersion, String javaVersion, String cherryApiVersion) {
		return BUILD_GRADLE.formatted(shadowVersion, projectGroup, projectVersion, javaVersion, cherryApiVersion);
	}

	/**
	 * Needed values:
	 * project group,
	 * project name
	 */
	private static final String PLUGIN_PROPERTIES =
		"""
		main-class = %s.%s
		""";

	public static String getPluginProperties(String projectGroup, String projectName) {
		return PLUGIN_PROPERTIES.formatted(projectGroup, projectName);
	}
}
