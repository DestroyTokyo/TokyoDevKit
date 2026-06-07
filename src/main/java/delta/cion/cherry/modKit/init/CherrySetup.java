package delta.cion.cherry.modKit.init;

import com.intellij.ide.wizard.AbstractNewProjectWizardStep;
import com.intellij.ide.wizard.NewProjectWizardStep;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.dsl.builder.Panel;
import com.intellij.ui.dsl.builder.Row;
import delta.cion.cherry.modKit.util.Constants;
import delta.cion.cherry.modKit.versions.*;
import kotlin.Unit;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CherrySetup extends AbstractNewProjectWizardStep {

	private static final List<CherryVersionRecord> _CherryVersionRecords = CherryVersion.parseCherryVersions();

	// UI fields
	private ComboBox<String> cherryCombo;
	private ComboBox<String> shadowCombo;
	private ComboBox<String> gradleCombo;
	private ComboBox<String> javaCombo;
	private JTextField packageField;

	// Data
	private String javaVersion;
	private String gradleVersion;
	private String cherryVersion;
	private String shadowVersion;
	private String packageName;

	private final List<String> javaVersions;
	private final List<String> gradleVersions;
	private final List<String> cherryVersions;
	private final List<String> shadowVersions;

	public CherrySetup(NewProjectWizardStep parent) {
		super(parent);

		javaVersions = JavaVersion.getJavaVersions();
		gradleVersions = GradleVersion.getGradleVersions();
		cherryVersions = getCherryVersions();
		shadowVersions = ShadowVersions.getShadowVersions();

		// default values
		javaVersion = javaVersions.isEmpty() ? "" : javaVersions.get(0);
		gradleVersion = gradleVersions.isEmpty() ? "" : gradleVersions.get(0);
		cherryVersion = cherryVersions.isEmpty() ? "" : cherryVersions.get(0);
		shadowVersion = shadowVersions.isEmpty() ? "" : shadowVersions.get(0);
		packageName = "";
	}

	private List<String> getCherryVersions() {
		List<String> versions = new ArrayList<>();
		for (CherryVersionRecord record : _CherryVersionRecords) {
			versions.add(record.version());
		}
		return versions;
	}

	private void initCombo(ComboBox<String> combo, List<String> items, java.util.function.Consumer<String> onSelected) {
		combo.setModel(new DefaultComboBoxModel<>(items.toArray(new String[0])));
		if (!items.isEmpty()) {
			combo.setSelectedItem(items.get(0));
		}
		combo.addActionListener(e -> {
			String selected = (String) combo.getSelectedItem();
			if (selected != null) {
				onSelected.accept(selected);
			}
		});
	}

	private void addComboRow(Panel p, String label, ComboBox<String> combo) {
		p.row(label, (Row row) -> {
			row.cell(combo);
			return Unit.INSTANCE;
		});
	}

	@Override
	public void setupUI(Panel panel) {
		javaCombo = new ComboBox<>();
		gradleCombo = new ComboBox<>();
		cherryCombo = new ComboBox<>();
		shadowCombo = new ComboBox<>();
		packageField = new JTextField();

		initCombo(javaCombo, javaVersions, v -> javaVersion = v);
		initCombo(gradleCombo, gradleVersions, v -> gradleVersion = v);
		initCombo(cherryCombo, cherryVersions, v -> cherryVersion = v);
		initCombo(shadowCombo, shadowVersions, v -> shadowVersion = v);

		packageField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) { updatePackageName(); }
			@Override
			public void removeUpdate(DocumentEvent e) { updatePackageName(); }
			@Override
			public void changedUpdate(DocumentEvent e) { updatePackageName(); }
			private void updatePackageName() {
				packageName = packageField.getText().trim();
			}
		});

		panel.group("Cherry Plugin Setup", true, p -> {
			addComboRow(p, "Java version:", javaCombo);
			addComboRow(p, "Gradle version:", gradleCombo);
			addComboRow(p, "Cherry version:", cherryCombo);
			addComboRow(p, "Shadow version:", shadowCombo);
			p.row("Project Package:", row -> {
				row.cell(packageField).resizableColumn();;
				return Unit.INSTANCE;
			});
			return Unit.INSTANCE;});
	}

	private void writeText(Path path, String content) throws IOException {
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
	}

	@Override
	public void setupProject(Project project) {
		String rootPath = project.getBasePath();
		if (rootPath == null || rootPath.isEmpty())
			throw new IllegalStateException("No path");

		Path root = Path.of(rootPath);
		String pkg = packageName.trim();

		if (!Constants.JAVA_PACKAGE_NAME.matcher(pkg).matches())
			throw new IllegalArgumentException("Java package is required");

		String packagePath = pkg.replace('.', '/');

		try {
			Files.createDirectories(root);
			writeText(root.resolve("settings.gradle.kts"),
				BaseFiles.getSettingsGradle(project.getName()));
			writeText(root.resolve("build.gradle.kts"),
				BaseFiles.getBuildGradle(shadowVersion, pkg, "0.0.0", javaVersion, cherryVersion));
			writeText(root.resolve("gradle/wrapper/gradle-wrapper.properties"),
				BaseFiles.getWrapperProperties(gradleVersion));
			writeText(root.resolve(".gitignore"),
				BaseFiles.getGitIgnore());

			String mainClassPath = "src/main/java/%s/%s.java".formatted(packagePath, project.getName());
			writeText(root.resolve(mainClassPath), JavaClasses.getMainClass(pkg, project.getName()));

			String pluginProperties = "src/main/resources/plugin.properties";
			writeText(root.resolve(pluginProperties), BaseFiles.getPluginProperties(pkg, project.getName()));

			Objects.requireNonNull(LocalFileSystem.getInstance()
					.refreshAndFindFileByNioFile(root))
				.refresh(false, true);

			runAfterOpened(project, openedProject -> {
				setProjectSdk(openedProject);
				ImportSpecBuilder specBuilder = new ImportSpecBuilder(openedProject, GradleConstants.SYSTEM_ID)
					.use(ProgressExecutionMode.IN_BACKGROUND_ASYNC);
				ExternalSystemUtil.refreshProject(root.toString(), specBuilder);
			});
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void runAfterOpened(Project project, java.util.function.Consumer<Project> action) {
		ApplicationManager.getApplication().invokeLater(() -> action.accept(project));
	}

	private void setProjectSdk(Project project) {
		Sdk sdk = configuredJava21Sdk();
		if (sdk == null) return;
		ApplicationManager.getApplication().invokeLater(() -> {
			if (!project.isDisposed()) {
				ApplicationManager.getApplication().runWriteAction(() -> {
					ProjectRootManager.getInstance(project).setProjectSdk(sdk);
				});
			}
		});
	}

	private Sdk configuredJava21Sdk() {
		JavaSdk javaSdk = JavaSdk.getInstance();
		for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
			if (sdk.getSdkType() == javaSdk &&
				Constants.JAVA_21_VERSION.matcher(sdk.getVersionString() != null ? sdk.getVersionString() : "").find()) {
				return sdk;
			}
		}
		return null;
	}
}
