package delta.cion.tokyo.devkit

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.*
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardChainStep.Companion.nextStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.observable.util.bindBooleanStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.dsl.builder.*
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import javax.swing.DefaultComboBoxModel
import javax.swing.Icon
import javax.swing.JComboBox
import javax.swing.JTextField
import kotlin.io.path.createDirectories

class TokyoDevKit : GeneratorNewProjectWizard {
	override val id: String = "tokyo_dev_kit"
	override val name: String = "TokyoDevKit"
	override val icon: Icon = IconLoader.getIcon("/icons/pluginIcon.svg", javaClass)
	override val ordinal: Int = 900
	override val description: String = "Build new Tokyo plugin!"

	override fun createStep(context: WizardContext): NewProjectWizardStep =
		RootNewProjectWizardStep(context)
			.nextStep(::NewProjectWizardBaseStep)
			.nextStep(::GitNewProjectWizardStep)
			.nextStep(::TokyoSetupStep)

	class Builder : GeneratorNewProjectWizardBuilderAdapter(TokyoDevKit())
}

private class TokyoSetupStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
	private val editionsMap: Map<String, List<String>> = loadTokyoEditionsMap()
	private val editionList: List<String> = editionsMap.keys.sorted()
	private val firstEdition: String = editionList.firstOrNull() ?: VERSION_LIST_UNAVAILABLE

	private val gradleVersions = currentGradleVersions()
	private val shadowVersions = currentMavenVersions(SHADOW_PLUGIN_METADATA_URL) { STABLE_MAVEN_VERSION.matches(it) }

	private val gradleVersionProperty = propertyGraph.property(gradleVersions.first())
	private val shadowVersionProperty = propertyGraph.property(shadowVersions.first())
	private val packageNameProperty = propertyGraph.property("")

	private val editionProperty = propertyGraph.property(firstEdition)
	private val versionProperty = propertyGraph.property(
		editionsMap[firstEdition]?.firstOrNull() ?: VERSION_LIST_UNAVAILABLE
	)

	private var selectedEdition: String by editionProperty
	private var selectedVersion: String by versionProperty

	private lateinit var versionComboBox: JComboBox<String>

	private val projectName = parent.context.projectName

	override fun setupUI(builder: Panel) {
		builder.group("Tokyo plugin setup") {
			row("Tokyo edition") {
				comboBox(editionList)
					.bindItem(editionProperty)
					.applyToComponent { comboBox ->
						comboBox.addActionListener {
							val newEdition = comboBox.selectedItem as? String ?: return@addActionListener
							val versions = editionsMap[newEdition] ?: emptyList()
							val model = DefaultComboBoxModel(versions.toTypedArray())
							versionComboBox.model = model
							val firstVersion = versions.firstOrNull() ?: VERSION_LIST_UNAVAILABLE
							versionComboBox.selectedItem = firstVersion
							versionProperty.set(firstVersion)
						}
					}
					.validationOnApply { validateEdition(it) }
			}
			row("Tokyo version") {
				val initialVersions = editionsMap[firstEdition] ?: emptyList()
				comboBox(initialVersions)
					.bindItem(versionProperty)
					.applyToComponent { versionComboBox = this }
					.validationOnApply { validateVersion(it) }
			}
			row("Gradle") {
				comboBox(gradleVersions)
					.bindItem(gradleVersionProperty)
					.validationOnApply { validateSelectedVersion(it, "Gradle") }
			}
			row("Shadow") {
				comboBox(shadowVersions)
					.bindItem(shadowVersionProperty)
					.validationOnApply { validateSelectedVersion(it, "Shadow") }
			}
			row("Java package") {
				textField()
					.bindText(packageNameProperty)
					.validationOnInput { validateJavaPackageName(it) }
					.validationOnApply { validateJavaPackageName(it) }
			}
		}
	}

	override fun setupProject(project: Project) {
		val base = baseData ?: error("Project base data is unavailable")
		val root = Path.of(base.contentEntryPath)
		val packageName = packageNameProperty.get().trim()
		require(packageName.matches(JAVA_PACKAGE_NAME)) { "Java package is required" }
		val packagePath = packageName.replace('.', '/')

		val edition = selectedEdition
		val version = selectedVersion
		require(version != VERSION_LIST_UNAVAILABLE) { "Invalid Tokyo version selected" }

		root.createDirectories()
		writeText(root.resolve("settings.gradle.kts"), settingsGradle(projectName))
		writeText(root.resolve("build.gradle.kts"), buildGradle(packageName, edition, version, shadowVersionProperty.get()))
		writeText(root.resolve("gradle.properties"), gradleProperties())
		writeText(root.resolve("gradle/wrapper/gradle-wrapper.properties"), wrapperProperties(gradleVersionProperty.get()))
		writeText(root.resolve(".gitignore"), gitIgnore())
		writeText(root.resolve("src/main/java/$packagePath/Main.java"), mainJava(packageName, projectName))
		writeText(root.resolve("src/main/resources/plugin.properties"), pluginProperties(packageName, projectName))

		LocalFileSystem.getInstance()
			.refreshAndFindFileByNioFile(root)
			?.refresh(false, true)
		runAfterOpened(project) { openedProject ->
			setProjectSdk(openedProject)
			val settings = GradleProjectSettings(root.toString()).apply {
				distributionType = DistributionType.DEFAULT_WRAPPED
			}
			ExternalSystemUtil.linkExternalProject(
				settings,
				ImportSpecBuilder(openedProject, GradleConstants.SYSTEM_ID)
					.use(ProgressExecutionMode.IN_BACKGROUND_ASYNC),
			)
		}
	}

	private fun validateEdition(field: JComboBox<String>): ValidationInfo? {
		val item = field.selectedItem as? String
		return if (item == null || item == VERSION_LIST_UNAVAILABLE) {
			ValidationInfo("Could not load Tokyo editions", field)
		} else null
	}

	private fun validateVersion(field: JComboBox<String>): ValidationInfo? {
		val item = field.selectedItem as? String
		return if (item == null || item == VERSION_LIST_UNAVAILABLE) {
			ValidationInfo("Could not load versions for selected edition", field)
		} else null
	}
}

private const val GRADLE_VERSIONS_URL = "https://services.gradle.org/versions/all"
private const val TOKYO_METADATA_URL = "https://tokyo.citory.net/jsons/versions.json"
private const val SHADOW_PLUGIN_METADATA_URL =
	"https://plugins.gradle.org/m2/com/gradleup/shadow/com.gradleup.shadow.gradle.plugin/maven-metadata.xml"
private const val VERSION_LIST_UNAVAILABLE = "Version list unavailable"
private val MIN_GRADLE_VERSION: GradleVersion = GradleVersion.version("8.5")
private val STABLE_GRADLE_VERSION = Regex("""\d+\.\d+(?:\.\d+)?""")
private val STABLE_MAVEN_VERSION = Regex("""\d+\.\d+\.\d+""")
private val MAVEN_METADATA_VERSION = Regex("""<version>([^<]+)</version>""")
private val JAVA_21_VERSION = Regex("""(?:^|\D)21(?:\D|$)""")
private val JAVA_PACKAGE_NAME = Regex("""[A-Za-z_$][A-Za-z\d_$]*(\.[A-Za-z_$][A-Za-z\d_$]*)*""")

private fun validateJavaPackageName(field: JTextField): ValidationInfo? = when {
	field.text.isBlank() -> ValidationInfo("Java package is required", field)
	!field.text.trim().matches(JAVA_PACKAGE_NAME) -> ValidationInfo("Enter a valid Java package", field)
	else -> null
}

private fun validateSelectedVersion(field: JComboBox<String>, name: String, required: Boolean = true): ValidationInfo? {
	val version = field.selectedItem as? String
	return when {
		!required -> null
		version.isNullOrBlank() || version == VERSION_LIST_UNAVAILABLE ->
			ValidationInfo("Could not load $name versions", field)
		else -> null
	}
}

private fun currentGradleVersions(): List<String> {
	val jsonString = runCatching {
		URI(GRADLE_VERSIONS_URL).toURL().readText()
	}.getOrNull() ?: return listOf(VERSION_LIST_UNAVAILABLE)

	return runCatching {
		val jsonArray = JSONArray(jsonString)
		val versions = mutableListOf<String>()
		for (i in 0 until jsonArray.length()) {
			val obj = jsonArray.getJSONObject(i)
			val version = obj.getString("version")
			val isRelease = !obj.optBoolean("snapshot", false) &&
				!obj.optBoolean("nightly", false) &&
				!obj.optBoolean("releaseNightly", false) &&
				!obj.optBoolean("activeRc", false) &&
				!obj.optBoolean("broken", false)
			if (isRelease && STABLE_GRADLE_VERSION.matches(version)) {
				val gradleVer = GradleVersion.version(version)
				if (gradleVer.baseVersion >= MIN_GRADLE_VERSION) {
					versions.add(version)
				}
			}
		}
		versions.distinct().sortedWith(compareByDescending { GradleVersion.version(it) })
	}.getOrDefault(emptyList()).ifEmpty { listOf(VERSION_LIST_UNAVAILABLE) }
}

private fun loadTokyoEditionsMap(): Map<String, List<String>> {
	val jsonString = runCatching {
		URI(TOKYO_METADATA_URL).toURL().readText()
	}.getOrNull() ?: return emptyMap()

	return runCatching {
		val jsonObject = JSONObject(jsonString)
		val map = mutableMapOf<String, List<String>>()
		for (key in jsonObject.keys()) {
			val versionsArray = jsonObject.getJSONArray(key)
			val versions = (0 until versionsArray.length()).map { versionsArray.getString(it) }
			val edition = key.substringAfterLast('/')
			map[edition] = versions.asReversed().distinct()
		}
		map
	}.getOrDefault(emptyMap())
}

private fun currentMavenVersions(url: String, versionFilter: (String) -> Boolean = { true }): List<String> {
	val versions = runCatching {
		MAVEN_METADATA_VERSION.findAll(URI(url).toURL().readText())
			.map { it.groupValues[1] }
			.filter(versionFilter)
			.toList()
			.asReversed()
			.distinct()
	}.getOrDefault(emptyList())
	return versions.ifEmpty { listOf(VERSION_LIST_UNAVAILABLE) }
}

private fun writeText(path: Path, content: String) {
	Files.createDirectories(path.parent)
	Files.writeString(path, content)
}

private fun setProjectSdk(project: Project) {
	val sdk = configuredJava21Sdk() ?: return
	val application = ApplicationManager.getApplication()
	application.invokeLater {
		if (!project.isDisposed) {
			application.runWriteAction {
				ProjectRootManager.getInstance(project).projectSdk = sdk
			}
		}
	}
}

private fun configuredJava21Sdk(): Sdk? {
	val javaSdk = JavaSdk.getInstance()
	return ProjectJdkTable.getInstance().allJdks.firstOrNull { sdk ->
		sdk.sdkType == javaSdk && JAVA_21_VERSION.containsMatchIn(sdk.versionString.orEmpty())
	}
}

private fun settingsGradle(projectName: String): String {
	return "rootProject.name = " + projectName.escapeKotlinString()
}

private fun buildGradle(packageName: String, edition: String, tokyoVersion: String, shadowVersion: String): String {
	return """
    |plugins {
    |    id("com.gradleup.shadow") version "$shadowVersion"
    |}
    |
    |group = "${packageName.escapeKotlinString()}"
    |
    |repositories {
    |    maven("https://tokyo.citory.net/")
    |    mavenCentral()
    |}
    |
    |java {
    |    toolchain {
    |        languageVersion = JavaLanguageVersion.of(21)
    |    }
    |}
    |
    |dependencies {
    |    compileOnly("delta.cion.tokyo:$edition:$tokyoVersion")
    |}
    |
    |tasks {
    |    build {
    |        dependsOn(shadowJar)
    |    }
    |
    |    withType<JavaCompile> {
    |        options.encoding = "UTF-8"
    |    }
    |}
    """.trimMargin()
}

private fun gradleProperties() = """
    |org.gradle.parallel=true
    |org.gradle.caching=false
    |
    |org.gradle.jvmargs=-Xmx4G
    |org.gradle.experimental.watching=false
    """.trimIndent() + "\n"

private fun wrapperProperties(version: String) = """
    |distributionBase=GRADLE_USER_HOME
    |distributionPath=wrapper/dists
    |distributionUrl=https\://services.gradle.org/distributions/gradle-$version-bin.zip
    |networkTimeout=10000
    |validateDistributionUrl=true
    |zipStoreBase=GRADLE_USER_HOME
    |zipStorePath=wrapper/dists
    """.trimIndent() + "\n"

private fun gitIgnore() = """
    |# ==================== #
    |# By: Lmao stupid Cion #
    |# ==================== #
    |
    |# <= Main =>
    |.env
    |*.iml
    |*.iws
    |.zed
    |.idea
    |
    |
    |# <= Gradle =>
    |.gradle
    |build
    |gradlew
    |gradlew.bat
    |
    |#   >> Contain
    |!.gitattributes
    """.trimIndent() + "\n"

private fun mainJava(packageName: String, projectName: String) = """
    |package $packageName;
    |
    |import delta.cion.tokyo.api.plugin.Plugin;
    |
    |public class $projectName extends Plugin {
    |
    |    public $projectName(String id, String name, String version) {
    |        super(id, name, version);
    |    }
    |
    |}""".trimIndent() + "\n"

private fun pluginProperties(packageName: String, projectName: String) = """
    |main-class = $packageName.$projectName
    |plugin-id = ${projectName.toLowerCase(Locale.ROOT)}
    |plugin-name = $projectName
    |
    |api-share = false
    """.trimIndent() + "\n"

private fun String.escapeKotlinString(): String =
	replace("\\", "\\\\")
		.replace("\"", "\\\"")
		.replace("$", "\${'$'}")
