package delta.cion.cherry.modkit;

import java.util.regex.Pattern;

public class Constants {

	private static final String VERSION_LIST_UNAVAILABLE = "Version list unavailable";
	private static final Pattern STABLE_MAVEN_VERSION = Pattern.compile("\\d+\\.\\d+\\.\\d+");
	private static final Pattern JAVA_PACKAGE_NAME = Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*(\\.[A-Za-z_$][A-Za-z\\d_$]*)*");
	private static final Pattern JAVA_25_VERSION = Pattern.compile("(?:^|\\D)25(?:\\D|$)");

}
