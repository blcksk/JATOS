package general.common;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import play.Configuration;
import play.Logger;
import play.api.Application;

@Singleton
public class Common {

	private static final String CLASS_NAME = Common.class.getSimpleName();

	/**
	 * JATOS version
	 */
	public static final String VERSION = Common.class.getPackage()
			.getImplementationVersion();

	/**
	 * Property name in application config for the path in the file system to
	 * the study assets root directory, the directory where all study assets are
	 * located
	 */
	private static final String PROPERTY_STUDY_ASSETS_ROOT_PATH = "jatos.studyAssetsRootPath";

	/**
	 * Default path in the file system to the study assets root directory in
	 * case it wasn't specified in the config
	 */
	private static final String DEFAULT_STUDY_ASSETS_ROOT_PATH = "study_assets_root";

	/**
	 * JATOS' absolute base path without trailing '/.'
	 */
	private final String basepath;

	/**
	 * Path in the file system to the study assets root directory. If the
	 * property is defined in the configuration file then use it as the base
	 * path. If property isn't defined, try in default study path instead.
	 */
	private final String studyAssetsRootPath;

	/**
	 * Is true if an in-memory database is used.
	 */
	private final boolean inMemoryDb;

	@Inject
	Common(Application application, Configuration configuration) {
		this.basepath = fillBasePath(application);
		this.studyAssetsRootPath = fillStudyAssetsRootPath(configuration);
		this.inMemoryDb = configuration.getString("db.default.url").contains(
				"jdbc:h2:mem:");
	}

	private String fillBasePath(Application application) {
		String tempBasePath = application.path().getAbsolutePath();
		if (tempBasePath.endsWith(File.separator + ".")) {
			tempBasePath = tempBasePath.substring(0, tempBasePath.length() - 2);
		}
		if (tempBasePath.endsWith(File.separator)) {
			tempBasePath = tempBasePath.substring(0, tempBasePath.length() - 1);
		}
		return tempBasePath;
	}

	private String fillStudyAssetsRootPath(Configuration configuration) {
		String tempStudyAssetsRootPath = configuration
				.getString(PROPERTY_STUDY_ASSETS_ROOT_PATH);
		if (tempStudyAssetsRootPath != null
				&& !tempStudyAssetsRootPath.trim().isEmpty()) {
			// Replace ~ with actual home directory
			tempStudyAssetsRootPath = tempStudyAssetsRootPath.replace("~",
					System.getProperty("user.home"));
			// Replace Unix-like file separator with actual system's one
			tempStudyAssetsRootPath = tempStudyAssetsRootPath.replace("/",
					File.separator);
		} else {
			tempStudyAssetsRootPath = DEFAULT_STUDY_ASSETS_ROOT_PATH;
		}

		// If relative path add JATOS' base path as prefix
		if (!tempStudyAssetsRootPath.startsWith(File.separator)) {
			tempStudyAssetsRootPath = this.basepath + File.separator
					+ tempStudyAssetsRootPath;
		}
		Logger.info(CLASS_NAME + ": Path to study assets directory is "
				+ tempStudyAssetsRootPath);
		return tempStudyAssetsRootPath;
	}

	public String getBasepath() {
		return basepath;
	}

	public String getStudyAssetsRootPath() {
		return studyAssetsRootPath;
	}

	public boolean isInMemoryDb() {
		return inMemoryDb;
	}

}
