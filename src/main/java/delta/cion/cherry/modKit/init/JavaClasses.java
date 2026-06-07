package delta.cion.cherry.modKit.init;

public class JavaClasses {

	/**
	 * Needed values:
	 * project group,
	 * project name x2.
	 */
	private static final String MAIN_CLASS =
		"""
		package %s;

		import delta.cion.cherry.api.Plugin;
		import org.slf4j.Logger;
		import org.slf4j.LoggerFactory;

		public class %s extends Plugin {

			private static final Logger LOGGER = LoggerFactory.getLogger(%s.class);

			public %s() {
				super("%s");
			}

			@Override
			public void onEnable() {
				LOGGER.info("This is your {} plugin for CherryServer!", getName());
			}

			@Override
			public void onDisable() {
				LOGGER.info("{} has shutdown!", getName());
			}
		}
		""";

	public static String getMainClass(String projectGroup, String projectName) {
		return MAIN_CLASS.formatted(
			projectGroup, projectName, projectName, projectName, projectName);
	}
}
