package delta.cion.cherry.modKit.versions;

import delta.cion.cherry.modKit.util.Constants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

	private static final Pattern FLAG_TRUE_PATTERN = Pattern.compile("\"([^\"]+)\"\\s*:\\s*true");

	public static List<String> getGradleVersions() {
		try {
			URL url = new URI(Constants.GRADLE_VERSIONS_URL).toURL();
			String content;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
				content = reader.lines().collect(Collectors.joining("\n"));
			}

			Matcher versionMatcher = Constants.GRADLE_VERSIONS_PATTERN.matcher(content);
			List<String> versions = new ArrayList<>();

			while (versionMatcher.find()) {
				String version = versionMatcher.group(1);
				if (isReleaseVersion(content, version) && Constants.STABLE_GRADLE_VERSION.matcher(version).matches()) {
					org.gradle.util.GradleVersion gv = org.gradle.util.GradleVersion.version(version);
					if (gv.getBaseVersion().compareTo(Constants.MIN_GRADLE_VERSION) >= 0) {
						versions.add(version);
					}
				}
			}

			versions = versions.stream()
				.distinct()
				.sorted((a, b) -> org.gradle.util.GradleVersion.version(b).compareTo(org.gradle.util.GradleVersion.version(a)))
				.collect(Collectors.toList());
			return versions;
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	private static boolean isReleaseVersion(String fullJson, String version) {
		String block = findBlockContaining(fullJson, version);
		if (block == null) return false;

		Matcher flagMatcher = FLAG_TRUE_PATTERN.matcher(block);
		while (flagMatcher.find()) {
			String flagName = flagMatcher.group(1);
			for (String unwanted : UNNEEDED) {
				if (unwanted.equals(flagName)) {
					return false;
				}
			}
		}
		return true;
	}

	private static String findBlockContaining(String json, String version) {
		String versionFieldPattern = "\"version\"\\s*:\\s*\"" + Pattern.quote(version) + "\"";
		Matcher matcher = Pattern.compile(versionFieldPattern).matcher(json);
		if (!matcher.find()) return null;

		int startPos = matcher.start();
		int braceStart = json.lastIndexOf('{', startPos);
		if (braceStart == -1) return null;

		int braceCount = 0;
		int braceEnd = -1;
		for (int i = braceStart; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '{') braceCount++;
			else if (c == '}') {
				braceCount--;
				if (braceCount == 0) {
					braceEnd = i;
					break;
				}
			}
		}
		if (braceEnd == -1) return null;

		return json.substring(braceStart, braceEnd + 1);
	}
}
