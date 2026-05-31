package delta.cion.cherry.modKit.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsing of CherryApi versions.
 * <br>Releases url located in Constants class
 */
public class GitHubParser {

	private static final String GITHUB_URL = Constants.CHERRY_VERSIONS_URL;
	private static final ArrayList<CherryVersion> VERSIONS = new ArrayList<>();

	public static List<CherryVersion> parseCherryVersions() {
		try {
			JSONArray cherryReleases = getAllVersions();
			if (cherryReleases != null)
				return parseReleases(cherryReleases);
			return null;
		} catch (Exception ignored) {return VERSIONS;}
	}

	private static JSONArray getAllVersions() throws Exception {
		JSONArray jsonList = new JSONArray();
		String nextPage = GITHUB_URL + "?per_page=100";

		while (nextPage != null) {
			HttpURLConnection c = (HttpURLConnection) new URI(nextPage).toURL().openConnection();
			c.setRequestMethod("GET");
			c.setRequestProperty("Accept", "application/vnd.github.v3+json");
			c.setRequestProperty("User-Agent", "CherryModKit/1.0");

			if (c.getResponseCode() != 200) return null;

			InputStreamReader inputStreamReader = new InputStreamReader(c.getInputStream());
			BufferedReader reader = new BufferedReader(inputStreamReader);
			StringBuilder response = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) response.append(line);
			reader.close();

			JSONArray page = new JSONArray(response.toString());
			for (int i = 0; i < page.length(); i++)
				jsonList.put(page.getJSONObject(i));

			String linkHeader = c.getHeaderField("Link");
			nextPage = getNewPage(linkHeader);
			c.disconnect();
		}
		return jsonList;
	}

	private static String getNewPage(String linkHeader) {
		if (linkHeader == null) return null;
		Pattern pattern = Pattern.compile("<([^>]+)>; rel=\"next\"");
		Matcher matcher = pattern.matcher(linkHeader);
		return matcher.find() ? matcher.group(1) : null;
	}

	private static List<CherryVersion> parseReleases(JSONArray releases) {
		var versions = new ArrayList<CherryVersion>();

		for (int i = 0; i < releases.length(); i++) {
			JSONObject release = releases.getJSONObject(i);
			JSONArray jars = release.getJSONArray("assets");
			if (jars == null) continue;

			String apiJar = null;
			String javadocJar = null;
			String sourcesJar = null;

			for (int ii = 0; ii < jars.length(); ii++) {
				JSONObject jar = jars.getJSONObject(ii);
				String jarName = jar.getString("name");
				if (!jarName.contains("CherryAPI-")) continue;

				String download = jar.getString("browser_download_url");
				if (jarName.contains("-javadoc")) javadocJar = download;
				else if (jarName.contains("-sources")) sourcesJar = download;
				else apiJar = download;
			}

			if (apiJar == null || javadocJar == null || sourcesJar == null) continue;
			if (apiJar.isBlank() || javadocJar.isBlank() || sourcesJar.isBlank()) continue;

			String version = release.getString("tag_name");
			versions.add(new CherryVersion(version, apiJar, javadocJar, sourcesJar));
		}
		return versions;
	}
}
