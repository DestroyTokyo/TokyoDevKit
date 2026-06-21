package delta.cion.cherry.modKit.util;

import com.intellij.openapi.util.IconLoader;
import org.gradle.util.GradleVersion;

import javax.swing.*;
import java.util.regex.Pattern;

public class Constants {

	public static final String PLUGIN_ID = "cherry_mod_kit";
	public static final String PLUGIN_NAME = "CherryModKit";
	public static final String PLUGIN_DESCRIPTION = "Simple IntellijIDEA extension for develop CherryServer plugins";

	public static final Icon PLUGIN_ICON = IconLoader.getIcon("/icons/cherry-logo-small.png", Constants.class);

	public static final String VERSION_LIST_UNAVAILABLE = "Version list unavailable";
	public static final Pattern STABLE_MAVEN_VERSION = Pattern.compile("\\d+\\.\\d+\\.\\d+");
	public static final Pattern JAVA_PACKAGE_NAME = Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*(\\.[A-Za-z_$][A-Za-z\\d_$]*)*");
	public static final Pattern JAVA_21_VERSION = Pattern.compile("(?:^|\\D)21(?:\\D|$)");


	// https://api.github.com/repos/CherryServer/CherryServer/releases
	// name	"v2.0.0-predemo" <-- Version but maybe needed parse tags (Tags always is version)
	// tag_name	"v2.0.0-predemo" <-- Also version but in tag (Tags always is version)
	// for (v : rel) list.add(getCherryVersion(v))
	// CherryVersion is interface bruh
	// Also I can cache it but idk..
	// Nah, also I can create json mirror with version like maven but my own
	public static final String CHERRY_VERSIONS_URL = "https://api.github.com/repos/DestroyTokyo/CherryServer/releases";

	public static final String GRADLE_VERSIONS_URL = "https://services.gradle.org/versions/all";
	public static final String SHADOW_PLUGIN_METADATA_URL = "https://plugins.gradle.org/m2/com/gradleup/shadow/com.gradleup.shadow.gradle.plugin/maven-metadata.xml";
	public static final Pattern STABLE_GRADLE_VERSION = Pattern.compile("\\d+\\.\\d+(?:\\.\\d+)?");
	public static final GradleVersion MIN_GRADLE_VERSION = GradleVersion.version("8.5");

	public static final Pattern GRADLE_VERSIONS_PATTERN = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");

}
