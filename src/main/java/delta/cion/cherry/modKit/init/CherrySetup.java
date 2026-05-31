package delta.cion.cherry.modKit.init;

import com.intellij.ide.wizard.AbstractNewProjectWizardStep;
import com.intellij.ide.wizard.NewProjectWizardStep;
import delta.cion.cherry.modKit.util.Constants;

import javax.swing.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CherrySetup extends AbstractNewProjectWizardStep {

	// UI fields
	private JComboBox<String> cherryCombo;
	private JComboBox<String> shadowCombo;
	private JComboBox<String> gradleCombo;
	private JComboBox<String> javaCombo;
	private JTextField packageField;

	// Data
	private String gradleVersion;
	private String cherryVersion;
	private String shadowVersion;
	private String javaVersion;
	private String packageName;

	private final List<String> gradleVersions;
	private final List<String> cherryVersions;
	private final List<String> shadowVersions;

	public CherrySetup(NewProjectWizardStep parent) {
		super(parent);
		gradleVersions = currentGradleVersions();
		cherryVersions = getCherryVersions();
		shadowVersions = currentMavenVersions(Constants.SHADOW_PLUGIN_METADATA_URL, v -> STABLE_MAVEN_VERSION.matcher(v).matches());

		// default values
		gradleVersion = gradleVersions.get(0);
		cherryVersions = cherryVersions.get(0);
		shadowVersion = shadowVersions.get(0);
		junitVersion = junitVersions.get(0);
		tests = true;
		packageName = "";
	}

	private static List<String> getCherryVersions() {
		List<String> releases = currentMavenVersions(Constants.CHERRY_VERSIONS_URL, v -> true)
			.stream().filter(v -> !v.equals(Constants.VERSION_LIST_UNAVAILABLE)).collect(Collectors.toList());
		List<String> snapshots = currentMavenVersions(Constants.CHERRY_VERSIONS_URL, v -> v.endsWith("-SNAPSHOT"))
			.stream().filter(v -> !v.equals(Constants.VERSION_LIST_UNAVAILABLE)).collect(Collectors.toList());
		Set<String> all = new LinkedHashSet<>();
		all.addAll(releases);
		all.addAll(snapshots);
		List<String> result = new ArrayList<>(all);
		return withUnavailableFallback(result);
	}
}
