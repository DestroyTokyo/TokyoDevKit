package delta.cion.cherry.modKit.versions;

import delta.cion.cherry.modKit.util.Constants;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GradleVersion {

	private static final String[] UNNEEDED = {"snapshot", "nightly", "releaseNightly", "activeRc", "broken"};

	public static List<String> getGradleVersions() {
		try {
			URL url = new URI(Constants.GRADLE_VERSIONS_URL).toURL();
			String content = new String(url.openStream().readAllBytes());
			Matcher m = Constants.GRADLE_VERSIONS_PATTERN.matcher(content);

			List<String> versions = new ArrayList<>();

			while (m.find()) {
				String v = m.group(1);
				if (isReleaseVersion(content, v) && Constants.STABLE_GRADLE_VERSION.matcher(v).matches()) {
					org.gradle.util.GradleVersion gv = org.gradle.util.GradleVersion.version(v);
					if (gv.getBaseVersion().compareTo(Constants.MIN_GRADLE_VERSION) >= 0)
						versions.add(v);
				}
			}
			versions = versions.stream().distinct()
				.sorted((a,b) -> org.gradle.util.GradleVersion.version(b).compareTo(org.gradle.util.GradleVersion.version(a)))
				.collect(Collectors.toList());
			return versions;
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}

	private static boolean isReleaseVersion(String fullJson, String version) {
		String block = findBlockContaining(fullJson, version);
		if (block == null) return false;

		for (String flag : UNNEEDED)
			if (Pattern.compile("\"" + flag + "\"\\s*:\\s*true").matcher(block).find())
				return false;
		return true;
	}

	private static String findBlockContaining(String json, String version) {
		var sub = "\"version\":\"%s\"".formatted(version);

		int indexOf = json.indexOf(sub);
		if (indexOf < 0) return null;

		int start = json.lastIndexOf('{', indexOf);
		int end = json.indexOf('}', indexOf);

		if (start >= 0 && end > start)
			return json.substring(start, end+1);

		return null;
	}
}
