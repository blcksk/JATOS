package publix;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import models.StudyModel;
import models.workers.GeneralSingleWorker;
import models.workers.JatosWorker;

import org.fest.assertions.Fail;
import org.junit.Test;

import play.mvc.Http;
import common.Global;
import controllers.gui.Users;
import controllers.publix.jatos.JatosErrorMessages;
import controllers.publix.jatos.JatosPublix;
import controllers.publix.jatos.JatosPublixUtils;
import exceptions.publix.ForbiddenPublixException;
import exceptions.publix.PublixException;

/**
 * @author Kristian Lange
 */
public class JatosPublixUtilsTest extends PublixUtilsTest<JatosWorker> {

	private JatosErrorMessages jatosErrorMessages;
	private JatosPublixUtils jatosPublixUtils;

	@Override
	public void before() throws Exception {
		super.before();
		jatosPublixUtils = Global.INJECTOR.getInstance(JatosPublixUtils.class);
		publixUtils = jatosPublixUtils;
		jatosErrorMessages = Global.INJECTOR
				.getInstance(JatosErrorMessages.class);
		errorMessages = jatosErrorMessages;
	}

	@Override
	public void after() throws Exception {
		super.before();
	}

	@Test
	public void checkRetrieveTypedWorker() throws NoSuchAlgorithmException,
			IOException, PublixException {
		JatosWorker retrievedWorker = publixUtils.retrieveTypedWorker(admin
				.getWorker().getId().toString());
		assertThat(retrievedWorker.getId())
				.isEqualTo(admin.getWorker().getId());
	}

	@Test
	public void checkRetrieveTypedWorkerWrongType()
			throws NoSuchAlgorithmException, IOException, PublixException {
		GeneralSingleWorker generalSingleWorker = new GeneralSingleWorker();
		entityManager.getTransaction().begin();
		workerDao.create(generalSingleWorker);
		entityManager.getTransaction().commit();

		try {
			publixUtils.retrieveTypedWorker(generalSingleWorker.getId()
					.toString());
			Fail.fail();
		} catch (PublixException e) {
			assertThat(e.getMessage()).isEqualTo(
					jatosErrorMessages.workerNotCorrectType(generalSingleWorker
							.getId()));
		}
	}

	@Test
	public void checkWorkerAllowedToDoStudy() throws NoSuchAlgorithmException,
			IOException, ForbiddenPublixException {
		mockContext();
		Http.Context.current().session()
				.put(Users.SESSION_EMAIL, admin.getEmail());

		StudyModel study = importExampleStudy();
		addStudy(study);

		publixUtils.checkWorkerAllowedToDoStudy(admin.getWorker(), study);

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkWorkerAllowedToDoStudyWrongWorkerType()
			throws NoSuchAlgorithmException, IOException {
		StudyModel study = importExampleStudy();
		study.removeAllowedWorker(admin.getWorker().getWorkerType());
		addStudy(study);

		// Study doesn't allow this worker type
		try {
			publixUtils.checkWorkerAllowedToDoStudy(admin.getWorker(), study);
			Fail.fail();
		} catch (PublixException e) {
			assertThat(e.getMessage()).isEqualTo(
					jatosErrorMessages.workerTypeNotAllowed(admin.getWorker()
							.getUIWorkerType()));
		}

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkWorkerAllowedToDoStudyNotMember()
			throws NoSuchAlgorithmException, IOException {
		StudyModel study = importExampleStudy();
		addStudy(study);

		entityManager.getTransaction().begin();
		study.removeMember(admin);
		entityManager.getTransaction().commit();

		// User has to be a member of this study
		try {
			publixUtils.checkWorkerAllowedToDoStudy(admin.getWorker(), study);
			Fail.fail();
		} catch (PublixException e) {
			assertThat(e.getMessage()).isEqualTo(
					errorMessages.workerNotAllowedStudy(admin.getWorker(),
							study.getId()));
		}

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkWorkerAllowedToDoStudyNotLoggedIn()
			throws NoSuchAlgorithmException, IOException {
		mockContext();

		StudyModel study = importExampleStudy();
		addStudy(study);

		// User has to be logged in
		try {
			publixUtils.checkWorkerAllowedToDoStudy(admin.getWorker(), study);
			Fail.fail();
		} catch (PublixException e) {
			assertThat(e.getMessage()).isEqualTo(
					errorMessages.workerNotAllowedStudy(admin.getWorker(),
							study.getId()));
		}

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkRetrieveJatosShowFromSession()
			throws ForbiddenPublixException {
		mockContext();
		Http.Context.current().session()
				.put(JatosPublix.JATOS_SHOW, JatosPublix.SHOW_STUDY);

		String jatosShow = jatosPublixUtils.retrieveJatosShowFromSession();

		assertThat(jatosShow).isEqualTo(JatosPublix.SHOW_STUDY);
	}

	@Test
	public void checkRetrieveJatosShowFromSessionFail()
			throws ForbiddenPublixException {
		mockContext();

		try {
			jatosPublixUtils.retrieveJatosShowFromSession();
			Fail.fail();
		} catch (PublixException e) {
			assertThat(e.getMessage())
					.isEqualTo(
							JatosErrorMessages.STUDY_OR_COMPONENT_NEVER_STARTED_FROM_JATOS);
		}
	}
}