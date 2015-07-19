package common;

import java.io.File;

import play.Play;

public class Common {

	/**
	 * JATOS' absolute base path without trailing '/.'
	 */
	public static final String BASEPATH = getBasePath();

	/**
	 * JATOS version
	 */
	public static final String VERSION = Common.class.getPackage()
			.getImplementationVersion();

	/**
	 * Is true if an in-memory database is used.
	 */
	public static final boolean IN_MEMORY_DB = Play.application().configuration()
			.getString("db.default.url").contains("jdbc:h2:mem:");

	private static String getBasePath() {
		String tempBasePath = Play.application().path().getAbsolutePath();
		if (tempBasePath.endsWith(File.separator + ".")) {
			tempBasePath = tempBasePath.substring(0, tempBasePath.length() - 2);
		}
		if (tempBasePath.endsWith(File.separator)) {
			tempBasePath = tempBasePath.substring(0, tempBasePath.length() - 1);
		}
		return tempBasePath;
	}
}