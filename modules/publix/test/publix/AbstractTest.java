package publix;

import static org.mockito.Mockito.mock;
import general.common.Common;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import javax.persistence.EntityManager;

import models.common.Study;
import models.common.StudyResult;
import models.common.StudyResult.StudyState;
import models.common.User;
import models.common.workers.Worker;

import org.apache.commons.io.FileUtils;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import play.GlobalSettings;
import play.Logger;
import play.api.mvc.RequestHeader;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.mvc.Http;
import play.test.FakeApplication;
import play.test.Helpers;
import play.test.TestServer;
import scala.Option;
import utils.common.HashUtils;
import utils.common.IOUtils;
import utils.common.StudyCloner;
import utils.common.StudyUploadUnmarshaller;
import utils.common.ZipUtil;
import daos.common.ComponentDao;
import daos.common.ComponentResultDao;
import daos.common.StudyDao;
import daos.common.StudyResultDao;
import daos.common.UserDao;
import daos.common.worker.WorkerDao;

/**
 * Abstract class for tests. Starts fake application and an in-memory DB.
 * 
 * @author Kristian Lange
 */
public abstract class AbstractTest {

	private static final String BASIC_EXAMPLE_STUDY_ZIP = "test/resources/basic_example_study.zip";

	private static final String CLASS_NAME = AbstractTest.class.getSimpleName();

	private static final String TEST_COMPONENT_JAC_PATH = "test/resources/quit_button.jac";
	private static final String TEST_COMPONENT_BKP_JAC_FILENAME = "quit_button_bkp.jac";

	private static Server dbH2Server;
	protected FakeApplication application;
	protected TestServer testServer;
	protected EntityManager entityManager;
	protected UserDao userDao;
	protected StudyDao studyDao;
	protected ComponentDao componentDao;
	protected WorkerDao workerDao;
	protected StudyResultDao studyResultDao;
	protected ComponentResultDao componentResultDao;
	protected StudyCloner studyCloner;
	protected User admin;

	public abstract void before() throws Exception;

	public abstract void after() throws Exception;

	@Before
	public void startApp() throws Exception {
		GlobalSettings global = (GlobalSettings) Class.forName(
				"gui.GuiTestGlobal").newInstance();

		application = Helpers.fakeApplication(global);
		if (testServer != null) {
			testServer.stop();
		}
		testServer = Helpers.testServer(play.api.test.Helpers.testServerPort(),
				application);
		testServer.start();

		// Use Guice dependency injection
		userDao = PublixTestGlobal.INJECTOR.getInstance(UserDao.class);
		studyDao = PublixTestGlobal.INJECTOR.getInstance(StudyDao.class);
		componentDao = PublixTestGlobal.INJECTOR
				.getInstance(ComponentDao.class);
		workerDao = PublixTestGlobal.INJECTOR.getInstance(WorkerDao.class);
		studyResultDao = PublixTestGlobal.INJECTOR
				.getInstance(StudyResultDao.class);
		componentResultDao = PublixTestGlobal.INJECTOR
				.getInstance(ComponentResultDao.class);
		studyCloner = PublixTestGlobal.INJECTOR.getInstance(StudyCloner.class);

		Option<JPAPlugin> jpaPlugin = application.getWrappedApplication()
				.plugin(JPAPlugin.class);
		entityManager = jpaPlugin.get().em("default");
		JPA.bindForCurrentThread(entityManager);

		// Get admin (admin is automatically created during initialisation)
		admin = userDao.findByEmail("admin");

		before();
	}

	@After
	public void stopApp() throws Exception {
		after();

		if (entityManager.isOpen()) {
			entityManager.close();
		}
		JPA.bindForCurrentThread(null);
		removeStudyAssetsRootDir();
		if (testServer != null) {
			testServer.stop();
		}
	}

	@BeforeClass
	public static void startDB() throws SQLException {
		dbH2Server = Server.createTcpServer().start();
		System.out.println("URL: jdbc:h2:" + dbH2Server.getURL()
				+ "/mem:test/jatos");
	}

	@AfterClass
	public static void stopDB() {
		dbH2Server.stop();
	}

	/**
	 * Mocks Play's Http.Context
	 */
	protected void mockContext() {
		Map<String, String> flashData = Collections.emptyMap();
		Map<String, Object> argData = Collections.emptyMap();
		Long id = 2L;
		RequestHeader header = mock(RequestHeader.class);
		Http.Request request = mock(Http.Request.class);
		Http.Context context = new Http.Context(id, header, request, flashData,
				flashData, argData);
		Http.Context.current.set(context);
		// Don't know why, but we have to bind entityManager again after mocking
		// the context
		JPA.bindForCurrentThread(entityManager);
	}

	protected static void removeStudyAssetsRootDir() throws IOException {
		File assetsRoot = new File(Common.STUDY_ASSETS_ROOT_PATH);
		if (assetsRoot.list().length > 0) {
			Logger.warn(CLASS_NAME
					+ ".removeStudyAssetsRootDir: Study assets root directory "
					+ Common.STUDY_ASSETS_ROOT_PATH
					+ " is not empty after finishing testing. This should not happen.");
		}
		FileUtils.deleteDirectory(assetsRoot);
	}

	protected Study importExampleStudy() throws IOException {
		File studyZip = new File(BASIC_EXAMPLE_STUDY_ZIP);
		File tempUnzippedStudyDir = ZipUtil.unzip(studyZip);
		File[] studyFileList = IOUtils.findFiles(tempUnzippedStudyDir, "",
				IOUtils.STUDY_FILE_SUFFIX);
		File studyFile = studyFileList[0];
		Study importedStudy = new StudyUploadUnmarshaller()
				.unmarshalling(studyFile);
		studyFile.delete();

		File[] dirArray = IOUtils.findDirectories(tempUnzippedStudyDir);
		IOUtils.moveStudyAssetsDir(dirArray[0], importedStudy.getDirName());

		tempUnzippedStudyDir.delete();
		return importedStudy;
	}

	protected synchronized File getExampleStudyFile() throws IOException {
		File studyFile = new File(BASIC_EXAMPLE_STUDY_ZIP);
		File studyFileBkp = new File(System.getProperty("java.io.tmpdir"),
				BASIC_EXAMPLE_STUDY_ZIP);
		FileUtils.copyFile(studyFile, studyFileBkp);
		return studyFileBkp;
	}

	/**
	 * Makes a backup of our component file
	 */
	protected synchronized File getExampleComponentFile() throws IOException {
		File componentFile = new File(TEST_COMPONENT_JAC_PATH);
		File componentFileBkp = new File(System.getProperty("java.io.tmpdir"),
				TEST_COMPONENT_BKP_JAC_FILENAME);
		FileUtils.copyFile(componentFile, componentFileBkp);
		return componentFileBkp;
	}

	protected synchronized Study cloneAndPersistStudy(Study studyToBeCloned)
			throws IOException {
		entityManager.getTransaction().begin();
		Study studyClone = studyCloner.clone(studyToBeCloned, admin);
		entityManager.getTransaction().commit();
		return studyClone;
	}

	protected synchronized User createAndPersistUser(String email, String name,
			String password) throws UnsupportedEncodingException,
			NoSuchAlgorithmException {
		String passwordHash = HashUtils.getHashMDFive(password);
		User user = new User(email, name, passwordHash);
		entityManager.getTransaction().begin();
		userDao.create(user);
		entityManager.getTransaction().commit();
		return user;
	}

	protected synchronized void removeStudy(Study study) throws IOException {
		IOUtils.removeStudyAssetsDir(study.getDirName());
		entityManager.getTransaction().begin();
		studyDao.remove(study);
		entityManager.getTransaction().commit();
	}

	protected synchronized void addStudy(Study study) {
		entityManager.getTransaction().begin();
		studyDao.create(study, admin);
		entityManager.getTransaction().commit();
	}

	protected synchronized void lockStudy(Study study) {
		entityManager.getTransaction().begin();
		study.setLocked(true);
		entityManager.getTransaction().commit();
	}

	protected synchronized void removeUser(Study studyClone, User user) {
		entityManager.getTransaction().begin();
		studyDao.findById(studyClone.getId()).removeUser(user);
		entityManager.getTransaction().commit();
	}

	protected void addWorker(Worker worker) {
		entityManager.getTransaction().begin();
		workerDao.create(worker);
		entityManager.getTransaction().commit();
	}

	protected void addStudyResult(Study study, Worker worker, StudyState state) {
		entityManager.getTransaction().begin();
		StudyResult studyResult = studyResultDao.create(study, worker);
		studyResult.setStudyState(state);
		// Have to set worker manually in test - don't know why
		studyResult.setWorker(worker);
		entityManager.getTransaction().commit();
	}

}