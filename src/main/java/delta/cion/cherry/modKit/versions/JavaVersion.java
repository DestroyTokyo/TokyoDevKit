package delta.cion.cherry.modKit.versions;

import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersion {

	public static List<String> getJavaVersions() {
		JavaSdk javaSdk = JavaSdk.getInstance();
		List<String> versions = new ArrayList<>();

		for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
			if (sdk.getSdkType() != javaSdk) continue;

			String version = sdk.getVersionString();
			if (version == null) continue;

			String java = extractJavaMajorVersion(version);
			if (java == null) continue;

			int javaVersion = Integer.parseInt(java);
			if (javaVersion < 21) continue;

			if (versions.contains(java)) continue;
			versions.add(java);
		}

		versions.sort(
			(a, b) ->
				Integer.compare(Integer.parseInt(b), Integer.parseInt(a)));

		if (versions.isEmpty()) return List.of("21");
		return versions;
	}

	private static String extractJavaMajorVersion(String versionString) {
		Pattern modernPattern = Pattern.compile("^(\\d+)(?:\\..*)?$");
		Matcher m = modernPattern.matcher(versionString.trim());
		if (m.find()) return m.group(1);
		return null;
	}
}
