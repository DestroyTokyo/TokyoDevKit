package delta.cion.cherry.modkit;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.dsl.builder.*;
import org.gradle.util.GradleVersion;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CherryModKitpig implements GeneratorNewProjectWizard {

    @Override
    public String getId() {
        return "minestom";
    }

    @Override
    public String getName() {
        return "Minestom";
    }

    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("/icons/minestom-logo-small.png", getClass());
    }

    @Override
    public int getOrdinal() {
        return 900;
    }

    @Override
    public String getDescription() {
        return "Create a Gradle Minestom server project.";
    }

    @Override
    public NewProjectWizardStep createStep(WizardContext context) {
        return new RootNewProjectWizardStep(context)
                .nextStep(NewProjectWizardBaseStep::new)
                .nextStep(GitNewProjectWizardStep::new)
                .nextStep(MinestomSetupStep::new);
    }

    public static class Builder extends GeneratorNewProjectWizardBuilderAdapter {
        public Builder() {
            super(new CherryModKitpig());
        }
    }

    // -------------------------------------------------------------------------
    //  MinestomSetupStep
    // -------------------------------------------------------------------------
    private static class MinestomSetupStep extends AbstractNewProjectWizardStep {
        private static final String VERSION_LIST_UNAVAILABLE = "Version list unavailable";
        private static final Pattern STABLE_MAVEN_VERSION = Pattern.compile("\\d+\\.\\d+\\.\\d+");
        private static final Pattern JAVA_PACKAGE_NAME = Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*(\\.[A-Za-z_$][A-Za-z\\d_$]*)*");
        private static final Pattern JAVA_25_VERSION = Pattern.compile("(?:^|\\D)25(?:\\D|$)");

        // UI fields
        private JComboBox<String> minestomCombo;
        private JComboBox<String> gradleCombo;
        private JComboBox<String> shadowCombo;
        private JComboBox<String> junitCombo;
        private JTextField packageField;
        private JCheckBox testsCheckBox;

        // Data
        private boolean tests;
        private String gradleVersion;
        private String minestomVersion;
        private String shadowVersion;
        private String junitVersion;
        private String packageName;

        private final List<String> gradleVersions;
        private final List<String> minestomVersions;
        private final List<String> shadowVersions;
        private final List<String> junitVersions;

        protected MinestomSetupStep(NewProjectWizardStep parent) {
            super(parent);
            gradleVersions = currentGradleVersions();
            minestomVersions = currentMinestomVersions();
            shadowVersions = currentMavenVersions(SHADOW_PLUGIN_METADATA_URL, v -> STABLE_MAVEN_VERSION.matcher(v).matches());
            junitVersions = currentMavenVersions(JUNIT_METADATA_URL, v -> STABLE_MAVEN_VERSION.matcher(v).matches());

            // default values
            gradleVersion = gradleVersions.get(0);
            minestomVersion = minestomVersions.get(0);
            shadowVersion = shadowVersions.get(0);
            junitVersion = junitVersions.get(0);
            tests = true;
            packageName = "";
        }

        @Override
        public void setupUI(Panel panel) {
            panel.group("Minestom", group -> {
                group.row("Minestom", row -> {
                    minestomCombo = new JComboBox<>(minestomVersions.toArray(new String[0]));
                    minestomCombo.setSelectedItem(minestomVersion);
                    minestomCombo.addActionListener(e -> minestomVersion = (String) minestomCombo.getSelectedItem());
                    row.component(minestomCombo)
                            .validationOnApply(combo -> validateSelectedVersion(combo, "Minestom"));
                });
                group.row("Gradle", row -> {
                    gradleCombo = new JComboBox<>(gradleVersions.toArray(new String[0]));
                    gradleCombo.setSelectedItem(gradleVersion);
                    gradleCombo.addActionListener(e -> gradleVersion = (String) gradleCombo.getSelectedItem());
                    row.component(gradleCombo)
                            .validationOnApply(combo -> validateSelectedVersion(combo, "Gradle"));
                });
                group.row("Shadow", row -> {
                    shadowCombo = new JComboBox<>(shadowVersions.toArray(new String[0]));
                    shadowCombo.setSelectedItem(shadowVersion);
                    shadowCombo.addActionListener(e -> shadowVersion = (String) shadowCombo.getSelectedItem());
                    row.component(shadowCombo)
                            .validationOnApply(combo -> validateSelectedVersion(combo, "Shadow"));
                });
                group.row("JUnit", row -> {
                    junitCombo = new JComboBox<>(junitVersions.toArray(new String[0]));
                    junitCombo.setSelectedItem(junitVersion);
                    junitCombo.addActionListener(e -> junitVersion = (String) junitCombo.getSelectedItem());
                    row.component(junitCombo)
                            .validationOnApply(combo -> validateSelectedVersion(combo, "JUnit", tests));
                });
                group.row("Java package", row -> {
                    packageField = new JTextField();
                    packageField.setText(packageName);
                    packageField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                        public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePackage(); }
                        public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePackage(); }
                        public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePackage(); }
                        private void updatePackage() { packageName = packageField.getText(); }
                    });
                    row.component(packageField)
                            .validationOnInput(field -> validateJavaPackageName(field))
                            .validationOnApply(field -> validateJavaPackageName(field));
                });
                group.row(row -> {
                    testsCheckBox = new JCheckBox("Set up tests");
                    testsCheckBox.setSelected(tests);
                    testsCheckBox.addActionListener(e -> tests = testsCheckBox.isSelected());
                    row.component(testsCheckBox);
                }).bottomGap(BottomGap.SMALL);
            });
        }

        @Override
        public void setupProject(Project project) {
            NewProjectWizardBaseData base = getBaseData();
            if (base == null) {
                throw new IllegalStateException("Project base data is unavailable");
            }
            Path root = Paths.get(base.getContentEntryPath());
            String pkg = packageName.trim();
            if (!JAVA_PACKAGE_NAME.matcher(pkg).matches()) {
                throw new IllegalArgumentException("Java package is required");
            }
            String packagePath = pkg.replace('.', '/');

            try {
                Files.createDirectories(root);
                writeText(root.resolve("settings.gradle.kts"), settingsGradle(base.getName(), minestomVersion));
                writeText(root.resolve("build.gradle.kts"), buildGradle(pkg, tests, minestomVersion, shadowVersion, junitVersion));
                writeText(root.resolve("gradle.properties"), gradleProperties());
                writeText(root.resolve("gradle/wrapper/gradle-wrapper.properties"), wrapperProperties(gradleVersion));
                writeText(root.resolve(".gitignore"), gitIgnore());
                writeText(root.resolve("src/main/java/" + packagePath + "/Main.java"), mainJava(pkg));
                if (tests) {
                    writeText(root.resolve("src/test/java/" + packagePath + "/FlatWorldTest.java"), flatWorldTestJava(pkg));
                }

                copyTemplateResource("/minestom-template/gradlew", root.resolve("gradlew"), true);
                copyTemplateResource("/minestom-template/gradlew.bat", root.resolve("gradlew.bat"), false);
                copyTemplateResource("/minestom-template/gradle/wrapper/gradle-wrapper.jar",
                        root.resolve("gradle/wrapper/gradle-wrapper.jar"), false);

                LocalFileSystem.getInstance()
                        .refreshAndFindFileByNioFile(root)
                        .refresh(false, true);

                runAfterOpened(project, openedProject -> {
                    setProjectSdk(openedProject);
                    GradleProjectSettings settings = new GradleProjectSettings(root.toString());
                    settings.setDistributionType(DistributionType.DEFAULT_WRAPPED);
                    ExternalSystemUtil.linkExternalProject(
                            settings,
                            new ImportSpecBuilder(openedProject, GradleConstants.SYSTEM_ID)
                                    .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
                    );
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // ---------------------------------------------------------------------
        //  Validation helpers
        // ---------------------------------------------------------------------
        private ValidationInfo validateJavaPackageName(JTextField field) {
            String text = field.getText().trim();
            if (text.isEmpty()) {
                return new ValidationInfo("Java package is required", field);
            }
            if (!JAVA_PACKAGE_NAME.matcher(text).matches()) {
                return new ValidationInfo("Enter a valid Java package", field);
            }
            return null;
        }

        private ValidationInfo validateSelectedVersion(JComboBox<String> combo, String name) {
            return validateSelectedVersion(combo, name, true);
        }

        private ValidationInfo validateSelectedVersion(JComboBox<String> combo, String name, boolean required) {
            if (!required) return null;
            String version = (String) combo.getSelectedItem();
            if (version == null || version.isBlank() || version.equals(VERSION_LIST_UNAVAILABLE)) {
                return new ValidationInfo("Could not load " + name + " versions", combo);
            }
            return null;
        }

        // ---------------------------------------------------------------------
        //  Version fetching
        // ---------------------------------------------------------------------
        private List<String> currentGradleVersions() {
            try {
                URL url = new URI(GRADLE_VERSIONS_URL).toURL();
                String content = new String(url.openStream().readAllBytes());
                // crude JSON parsing: find "version":"..."
                Pattern versionPattern = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
                Matcher m = versionPattern.matcher(content);
                List<String> versions = new ArrayList<>();
                while (m.find()) {
                    String v = m.group(1);
                    // check release and stable
                    if (isReleaseVersion(content, v) && STABLE_GRADLE_VERSION.matcher(v).matches()) {
                        GradleVersion gv = GradleVersion.version(v);
                        if (gv.getBaseVersion().compareTo(MIN_GRADLE_VERSION) >= 0) {
                            versions.add(v);
                        }
                    }
                }
                versions = versions.stream().distinct()
                        .sorted((a,b) -> GradleVersion.version(b).compareTo(GradleVersion.version(a)))
                        .collect(Collectors.toList());
                return withUnavailableFallback(versions);
            } catch (Exception e) {
                return withUnavailableFallback(Collections.emptyList());
            }
        }

        private boolean isReleaseVersion(String fullJson, String version) {
            // find the object block containing this version and check flags
            String block = findBlockContaining(fullJson, "\"version\":\"" + version + "\"");
            if (block == null) return false;
            String[] flags = {"snapshot", "nightly", "releaseNightly", "activeRc", "broken"};
            for (String flag : flags) {
                if (Pattern.compile("\"" + flag + "\"\\s*:\\s*true").matcher(block).find()) {
                    return false;
                }
            }
            return true;
        }

        private String findBlockContaining(String json, String sub) {
            int idx = json.indexOf(sub);
            if (idx < 0) return null;
            int start = json.lastIndexOf('{', idx);
            int end = json.indexOf('}', idx);
            if (start >= 0 && end > start) {
                return json.substring(start, end+1);
            }
            return null;
        }

        private List<String> currentMinestomVersions() {
            List<String> releases = currentMavenVersions(MINESTOM_RELEASE_METADATA_URL, v -> true)
                    .stream().filter(v -> !v.equals(VERSION_LIST_UNAVAILABLE)).collect(Collectors.toList());
            List<String> snapshots = currentMavenVersions(MINESTOM_SNAPSHOT_METADATA_URL, v -> v.endsWith("-SNAPSHOT"))
                    .stream().filter(v -> !v.equals(VERSION_LIST_UNAVAILABLE)).collect(Collectors.toList());
            Set<String> all = new LinkedHashSet<>();
            all.addAll(releases);
            all.addAll(snapshots);
            List<String> result = new ArrayList<>(all);
            return withUnavailableFallback(result);
        }

        private List<String> currentMavenVersions(String url, java.util.function.Predicate<String> filter) {
            try {
                URL u = new URI(url).toURL();
                String content = new String(u.openStream().readAllBytes());
                Pattern versionPattern = Pattern.compile("<version>([^<]+)</version>");
                Matcher m = versionPattern.matcher(content);
                List<String> versions = new ArrayList<>();
                while (m.find()) {
                    String v = m.group(1);
                    if (filter.test(v)) {
                        versions.add(v);
                    }
                }
                Collections.reverse(versions);
                versions = versions.stream().distinct().collect(Collectors.toList());
                return withUnavailableFallback(versions);
            } catch (Exception e) {
                return withUnavailableFallback(Collections.emptyList());
            }
        }

        private List<String> withUnavailableFallback(List<String> list) {
            if (list.isEmpty()) return Collections.singletonList(VERSION_LIST_UNAVAILABLE);
            return list;
        }

        // ---------------------------------------------------------------------
        //  File writing helpers
        // ---------------------------------------------------------------------
        private void writeText(Path path, String content) throws IOException {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        }

        private void copyTemplateResource(String resourcePath, Path target, boolean executable) throws IOException {
            Files.createDirectories(target.getParent());
            try (InputStream is = CherryModKitpig.class.getResourceAsStream(resourcePath)) {
                if (is == null) throw new IllegalStateException("Missing template resource: " + resourcePath);
                Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
            }
            if (executable) {
                target.toFile().setExecutable(true, false);
            }
        }

        private void setProjectSdk(Project project) {
            Sdk sdk = configuredJava25Sdk();
            if (sdk == null) return;
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!project.isDisposed()) {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        ProjectRootManager.getInstance(project).setProjectSdk(sdk);
                    });
                }
            });
        }

        private Sdk configuredJava25Sdk() {
            JavaSdk javaSdk = JavaSdk.getInstance();
            for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
                if (sdk.getSdkType() == javaSdk && JAVA_25_VERSION.matcher(sdk.getVersionString() != null ? sdk.getVersionString() : "").find()) {
                    return sdk;
                }
            }
            return null;
        }

        private void runAfterOpened(Project project, java.util.function.Consumer<Project> action) {
            // Simple implementation: just invoke later, the project is already opened in wizard
            ApplicationManager.getApplication().invokeLater(() -> action.accept(project));
        }

        // ---------------------------------------------------------------------
        //  Template generators
        // ---------------------------------------------------------------------
        private String settingsGradle(String projectName, String minestomVersion) {
            String snapshotRepo = "";
            if (minestomVersion.endsWith("-SNAPSHOT")) {
                snapshotRepo = """
                        maven(url = "$MINESTOM_SNAPSHOT_REPOSITORY_URL") {
                            content {
                                includeModule("net.minestom", "minestom")
                                includeModule("net.minestom", "testing")
                            }
                        }
                        """;
            }
            String repos = snapshotRepo + "        mavenCentral()";
            return """
                    pluginManagement {
                        repositories {
                            gradlePluginPortal()
                            mavenCentral()
                        }
                    }

                    dependencyResolutionManagement {
                        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                        repositories {
                    """ + repos + """
                        }
                    }

                    rootProject.name = "%s"
                    """.formatted(escapeKotlinString(projectName));
        }

        private String buildGradle(String packageName, boolean tests, String minestomVersion,
                                   String shadowVersion, String junitVersion) {
            String mainClassName = packageName + ".Main";
            String testDeps = "";
            if (tests) {
                testDeps = """
                        testImplementation("net.minestom:testing:%s")
                        testImplementation("org.junit.jupiter:junit-jupiter:%s")
                        """.formatted(minestomVersion, junitVersion);
            }
            String testTask = tests ? """

                    tasks.test {
                        useJUnitPlatform()
                    }
                    """ : "";
            String shadowTasks = """

                    tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
                        mergeServiceFiles()
                        archiveClassifier.set("")
                    }

                    tasks.named("build") {
                        dependsOn(tasks.named("shadowJar"))
                    }
                    """;
            return """
                    plugins {
                        application
                        id("com.gradleup.shadow") version "%s"
                    }

                    group = "%s"

                    dependencies {
                        implementation("net.minestom:minestom:%s")
                    %s
                    }

                    java {
                        toolchain {
                            languageVersion = JavaLanguageVersion.of(25)
                        }
                    }

                    application {
                        mainClass = "%s"
                    }

                    tasks.jar {
                        manifest {
                            attributes["Main-Class"] = "%s"
                        }
                    }%s%s
                    """.formatted(shadowVersion, escapeKotlinString(packageName), minestomVersion,
                            testDeps, mainClassName, mainClassName, testTask, shadowTasks);
        }

        private String gradleProperties() {
            return """
                    org.gradle.configuration-cache=true
                    org.gradle.caching=true
                    """;
        }

        private String wrapperProperties(String version) {
            return """
                    distributionBase=GRADLE_USER_HOME
                    distributionPath=wrapper/dists
                    distributionUrl=https\\://services.gradle.org/distributions/gradle-%s-bin.zip
                    networkTimeout=10000
                    validateDistributionUrl=true
                    zipStoreBase=GRADLE_USER_HOME
                    zipStorePath=wrapper/dists
                    """.formatted(version);
        }

        private String gitIgnore() {
            return """
                    .gradle/
                    build/
                    out/
                    *.iml
                    .idea/
                    """;
        }

        private String mainJava(String packageName) {
            return """
                    package %s;

                    import net.minestom.server.MinecraftServer;
                    import net.minestom.server.coordinate.Pos;
                    import net.minestom.server.entity.Player;
                    import net.minestom.server.event.GlobalEventHandler;
                    import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
                    import net.minestom.server.instance.InstanceContainer;
                    import net.minestom.server.instance.InstanceManager;
                    import net.minestom.server.instance.LightingChunk;
                    import net.minestom.server.instance.block.Block;

                    public final class Main {
                        static void main() {
                            MinecraftServer minecraftServer = MinecraftServer.init();
                            InstanceContainer instanceContainer = createInstance();

                            GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
                            globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
                                Player player = event.getPlayer();
                                event.setSpawningInstance(instanceContainer);
                                player.setRespawnPoint(new Pos(0, 42, 0));
                            });

                            minecraftServer.start("0.0.0.0", 25565);
                        }

                        public static InstanceContainer createInstance() {
                            InstanceManager instanceManager = MinecraftServer.getInstanceManager();
                            InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
                            instanceContainer.setChunkSupplier(LightingChunk::new);
                            instanceContainer.setGenerator(unit -> {
                                unit.modifier().fillHeight(0, 1, Block.BEDROCK);
                                unit.modifier().fillHeight(1, 3, Block.DIRT);
                                unit.modifier().fillHeight(3, 4, Block.GRASS_BLOCK);
                            });
                            return instanceContainer;
                        }
                    }
                    """.formatted(packageName);
        }

        private String flatWorldTestJava(String packageName) {
            return """
                    package %s;

                    import net.minestom.server.instance.InstanceContainer;
                    import net.minestom.server.instance.block.Block;
                    import net.minestom.testing.Env;
                    import net.minestom.testing.EnvTest;
                    import org.junit.jupiter.api.Test;

                    import static org.junit.jupiter.api.Assertions.assertEquals;

                    @EnvTest
                    class FlatWorldTest {
                        @Test
                        void generatesVanillaFlatWorld(Env env) {
                            InstanceContainer instance = Main.createInstance();
                            try {
                                instance.loadChunk(0, 0).join();

                                assertEquals(Block.BEDROCK, instance.getBlock(0, 0, 0));
                                assertEquals(Block.DIRT, instance.getBlock(0, 1, 0));
                                assertEquals(Block.DIRT, instance.getBlock(0, 2, 0));
                                assertEquals(Block.GRASS_BLOCK, instance.getBlock(0, 3, 0));
                                assertEquals(Block.AIR, instance.getBlock(0, 4, 0));
                            } finally {
                                env.destroyInstance(instance);
                            }
                        }
                    }
                    """.formatted(packageName);
        }

        private String escapeKotlinString(String s) {
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("$", "${'$'}");
        }
    }

    // Constants
    private static final String GRADLE_VERSIONS_URL = "https://services.gradle.org/versions/all";
    private static final String MINESTOM_RELEASE_METADATA_URL = "https://repo.maven.apache.org/maven2/net/minestom/minestom/maven-metadata.xml";
    private static final String MINESTOM_SNAPSHOT_METADATA_URL = "https://central.sonatype.com/repository/maven-snapshots/net/minestom/minestom/maven-metadata.xml";
    private static final String MINESTOM_SNAPSHOT_REPOSITORY_URL = "https://central.sonatype.com/repository/maven-snapshots/";
    private static final String SHADOW_PLUGIN_METADATA_URL = "https://plugins.gradle.org/m2/com/gradleup/shadow/com.gradleup.shadow.gradle.plugin/maven-metadata.xml";
    private static final String JUNIT_METADATA_URL = "https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter/maven-metadata.xml";
    private static final Pattern STABLE_GRADLE_VERSION = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?");
    private static final GradleVersion MIN_GRADLE_VERSION = GradleVersion.version("9.1");
}
