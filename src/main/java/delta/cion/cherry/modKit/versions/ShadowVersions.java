package delta.cion.cherry.modKit.versions;

import delta.cion.cherry.modKit.util.Constants;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ShadowVersions {

	public static List<String> getShadowVersions() {
		return currentMavenVersions(Constants.SHADOW_PLUGIN_METADATA_URL, v -> Constants.STABLE_MAVEN_VERSION.matcher(v).matches());
	}

	private static List<String> currentMavenVersions(String url, Predicate<String> filter) {
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

	private static List<String> withUnavailableFallback(List<String> list) {
		if (list.isEmpty()) return Collections.singletonList(Constants.VERSION_LIST_UNAVAILABLE);
		return list;
	}

}
