package delta.cion.tokyo.modKit.versions;

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
		List<String> entries = new ArrayList<>();

		for (Sdk sdk : ProjectJdkTable.getInstance().getAllJdks()) {
			if (sdk.getSdkType() != javaSdk) continue;

			String versionString = sdk.getVersionString();
			if (versionString == null) continue;

			String major = extractJavaMajorVersion(versionString);
			if (major == null) continue;

			int javaVersion = Integer.parseInt(major);
			if (javaVersion < 21) continue;

			entries.add(major + " (" + sdk.getName() + ")");
		}

		entries.sort((a, b) -> {
			int verA = extractMajorFromDisplay(a);
			int verB = extractMajorFromDisplay(b);
			return Integer.compare(verB, verA);
		});

		if (entries.isEmpty()) return List.of("21 (No JDK found)");
		return entries;
	}

	private static String extractJavaMajorVersion(String versionString) {
		Matcher m = Pattern.compile("^(\\d+)").matcher(versionString.trim());
		return m.find() ? m.group(1) : null;
	}

	private static int extractMajorFromDisplay(String display) {
		Matcher m = Pattern.compile("^(\\d+)").matcher(display);
		return m.find() ? Integer.parseInt(m.group(1)) : 0;
	}
}
