package gui.services;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.fest.assertions.Fail;
import org.junit.Test;

import exceptions.gui.BadRequestException;
import exceptions.gui.ForbiddenException;
import exceptions.gui.NotFoundException;
import exceptions.publix.ForbiddenReloadException;
import general.AbstractTest;
import general.common.MessagesStrings;
import models.common.ComponentResult;
import models.common.Study;
import models.common.StudyResult;
import services.gui.ResultDataStringGenerator;
import services.publix.workers.JatosPublixUtils;

/**
 * Tests ResultDataStringGenerator
 * 
 * @author Kristian Lange
 */
public class ResultDataStringGeneratorTests extends AbstractTest {

	private ResultDataStringGenerator resultDataStringGenerator;
	private JatosPublixUtils jatosPublixUtils;

	@Override
	public void before() throws Exception {
		resultDataStringGenerator = application.injector()
				.instanceOf(ResultDataStringGenerator.class);
		jatosPublixUtils = application.injector()
				.instanceOf(JatosPublixUtils.class);
	}

	@Override
	public void after() throws Exception {
	}

	@Test
	public void simpleCheck() {
		int a = 1 + 1;
		assertThat(a).isEqualTo(2);
	}

	@Test
	public void checkForWorker() throws IOException, ForbiddenException,
			BadRequestException, ForbiddenReloadException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoStudyResults(study);

		String resultData = resultDataStringGenerator.forWorker(admin,
				admin.getWorker());
		assertThat(resultData)
				.isEqualTo("1. StudyResult, 1. Component, 1. ComponentResult\n"
						+ "1. StudyResult, 1. Component, 2. ComponentResult\n"
						+ "2. StudyResult, 1. Component, 1. ComponentResult\n"
						+ "2. StudyResult, 1. Component, 2. ComponentResult\n"
						+ "2. StudyResult, 2. Component, 1. ComponentResult\n"
						+ "2. StudyResult, 2. Component, 2. ComponentResult");

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkForStudy() throws IOException, ForbiddenException,
			BadRequestException, ForbiddenReloadException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoStudyResults(study);

		String resultData = resultDataStringGenerator.forStudy(admin, study);
		assertThat(resultData)
				.isEqualTo("1. StudyResult, 1. Component, 1. ComponentResult\n"
						+ "1. StudyResult, 1. Component, 2. ComponentResult\n"
						+ "2. StudyResult, 1. Component, 1. ComponentResult\n"
						+ "2. StudyResult, 1. Component, 2. ComponentResult\n"
						+ "2. StudyResult, 2. Component, 1. ComponentResult\n"
						+ "2. StudyResult, 2. Component, 2. ComponentResult");

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkForComponent() throws IOException, ForbiddenException,
			BadRequestException, ForbiddenReloadException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoStudyResults(study);

		String resultData = resultDataStringGenerator.forComponent(admin,
				study.getFirstComponent());
		assertThat(resultData)
				.isEqualTo("1. StudyResult, 1. Component, 1. ComponentResult\n"
						+ "1. StudyResult, 1. Component, 2. ComponentResult\n"
						+ "2. StudyResult, 1. Component, 1. ComponentResult\n"
						+ "2. StudyResult, 1. Component, 2. ComponentResult");

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkFromListOfComponentResultIds()
			throws BadRequestException, ForbiddenException, IOException,
			NotFoundException, ForbiddenReloadException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoComponentResultsWithData(study);

		String resultData = resultDataStringGenerator
				.fromListOfComponentResultIds("1, 2", admin);
		assertThat(resultData).isEqualTo(
				"Thats a first component result.\nThats a second component result.");

		// Clean-up
		removeStudy(study);
	}

	private void createTwoComponentResultsWithData(Study study)
			throws ForbiddenReloadException {
		entityManager.getTransaction().begin();
		StudyResult studyResult = resultCreator.createStudyResult(study,
				study.getDefaultBatch(), admin.getWorker());
		// Have to set worker manually in test - don't know why
		studyResult.setWorker(admin.getWorker());
		ComponentResult componentResult1 = jatosPublixUtils
				.startComponent(study.getFirstComponent(), studyResult);
		componentResult1.setData("Thats a first component result.");
		// Have to set study manually in test - don't know why
		componentResult1.getComponent().setStudy(study);
		ComponentResult componentResult2 = jatosPublixUtils
				.startComponent(study.getFirstComponent(), studyResult);
		componentResult2.setData("Thats a second component result.");
		// Have to set study manually in test - don't know why
		componentResult2.getComponent().setStudy(study);
		entityManager.getTransaction().commit();
	}

	@Test
	public void checkFromListOfComponentResultIdsEmpty()
			throws BadRequestException, ForbiddenException, IOException,
			ForbiddenReloadException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoComponentResultsWithoutData(study);

		try {
			resultDataStringGenerator.fromListOfComponentResultIds("1, 2",
					admin);
		} catch (NotFoundException e) {
			assertThat(e.getMessage())
					.isEqualTo(MessagesStrings.componentResultNotExist(1l));
		}

		// Clean-up
		removeStudy(study);
	}

	private void createTwoComponentResultsWithoutData(Study study)
			throws ForbiddenReloadException {
		entityManager.getTransaction().begin();
		StudyResult studyResult = resultCreator.createStudyResult(study,
				study.getDefaultBatch(), admin.getWorker());
		// Have to set worker manually in test - don't know why
		studyResult.setWorker(admin.getWorker());
		ComponentResult componentResult = jatosPublixUtils
				.startComponent(study.getFirstComponent(), studyResult);
		// Have to set study manually in test - don't know why
		componentResult.getComponent().setStudy(study);
		componentResult = jatosPublixUtils
				.startComponent(study.getFirstComponent(), studyResult);
		// Have to set study manually in test - don't know why
		componentResult.getComponent().setStudy(study);
		entityManager.getTransaction().commit();
	}

	@Test
	public void checkFromListOfStudyResultIds()
			throws IOException, BadRequestException, NotFoundException,
			ForbiddenException, ForbiddenReloadException {
		Study study = importExampleStudy();
		addStudy(study);
		createTwoStudyResults(study);

		String resultData = null;
		try {
			resultData = resultDataStringGenerator
					.fromListOfStudyResultIds("1, 2", admin);
		} catch (NotFoundException e) {
			Fail.fail();
		}
		assertThat(resultData)
				.isEqualTo("1. StudyResult, 1. Component, 1. ComponentResult\n"
						+ "1. StudyResult, 1. Component, 2. ComponentResult\n"
						+ "2. StudyResult, 1. Component, 1. ComponentResult\n"
						+ "2. StudyResult, 1. Component, 2. ComponentResult\n"
						+ "2. StudyResult, 2. Component, 1. ComponentResult\n"
						+ "2. StudyResult, 2. Component, 2. ComponentResult");

		// Clean-up
		removeStudy(study);
	}

	private void createTwoStudyResults(Study study)
			throws ForbiddenReloadException {
		entityManager.getTransaction().begin();
		StudyResult studyResult1 = resultCreator.createStudyResult(study,
				study.getDefaultBatch(), admin.getWorker());
		// Have to set worker manually in test - don't know why
		studyResult1.setWorker(admin.getWorker());
		ComponentResult componentResult11 = jatosPublixUtils
				.startComponent(study.getFirstComponent(), studyResult1);
		componentResult11
				.setData("1. StudyResult, 1. Component, 1. ComponentResult");
		ComponentResult componentResult12 = jatosPublixUtils
				.startComponent(study.getFirstComponent(), studyResult1);
		componentResult12
				.setData("1. StudyResult, 1. Component, 2. ComponentResult");

		StudyResult studyResult2 = resultCreator.createStudyResult(study,
				study.getBatchList().get(0), admin.getWorker());
		// // Have to set worker manually in test - don't know why
		studyResult2.setWorker(admin.getWorker());
		ComponentResult componentResult211 = jatosPublixUtils
				.startComponent(study.getFirstComponent(), studyResult2);
		componentResult211
				.setData("2. StudyResult, 1. Component, 1. ComponentResult");
		ComponentResult componentResult212 = jatosPublixUtils
				.startComponent(study.getFirstComponent(), studyResult2);
		componentResult212
				.setData("2. StudyResult, 1. Component, 2. ComponentResult");
		ComponentResult componentResult221 = jatosPublixUtils
				.startComponent(study.getComponent(2), studyResult2);
		componentResult221
				.setData("2. StudyResult, 2. Component, 1. ComponentResult");
		ComponentResult componentResult222 = jatosPublixUtils
				.startComponent(study.getComponent(2), studyResult2);
		componentResult222
				.setData("2. StudyResult, 2. Component, 2. ComponentResult");

		// Have to set study manually in test - don't know why
		componentResult11.getComponent().setStudy(study);
		componentResult12.getComponent().setStudy(study);
		componentResult211.getComponent().setStudy(study);
		componentResult212.getComponent().setStudy(study);
		componentResult221.getComponent().setStudy(study);
		componentResult222.getComponent().setStudy(study);
		entityManager.getTransaction().commit();
	}

	@Test
	public void checkFromListOfStudyResultIdsEmpty()
			throws NoSuchAlgorithmException, IOException, BadRequestException,
			ForbiddenException {
		Study study = importExampleStudy();
		addStudy(study);

		// Never added any results
		try {
			resultDataStringGenerator.fromListOfStudyResultIds("1, 2", admin);
		} catch (NotFoundException e) {
			assertThat(e.getMessage())
					.isEqualTo(MessagesStrings.studyResultNotExist(1l));
		}

		// Clean-up
		removeStudy(study);
	}

}
