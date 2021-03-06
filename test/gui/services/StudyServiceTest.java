package gui.services;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.fest.assertions.Fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import exceptions.gui.BadRequestException;
import exceptions.gui.ForbiddenException;
import general.AbstractTest;
import general.common.MessagesStrings;
import models.common.Component;
import models.common.Study;
import models.common.User;
import models.gui.StudyProperties;

/**
 * Tests StudyService
 * 
 * @author Kristian Lange
 */
public class StudyServiceTest extends AbstractTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Override
	public void before() throws Exception {
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
	public void checkCloneStudy() throws NoSuchAlgorithmException, IOException {
		Study study = importExampleStudy();
		addStudy(study);
		Study clone = cloneAndPersistStudy(study);
		Study cloneInDb = studyDao.findByUuid(clone.getUuid());

		// Equal
		assertThat(cloneInDb.getComponentList().size())
				.isEqualTo(study.getComponentList().size());
		assertThat(cloneInDb.getFirstComponent().getTitle())
				.isEqualTo(study.getFirstComponent().getTitle());
		assertThat(cloneInDb.getLastComponent().getTitle())
				.isEqualTo(study.getLastComponent().getTitle());
		assertThat(cloneInDb.getDate()).isEqualTo(study.getDate());
		assertThat(cloneInDb.getDescription())
				.isEqualTo(study.getDescription());
		assertThat(cloneInDb.getComments()).isEqualTo(study.getComments());
		assertThat(cloneInDb.getJsonData()).isEqualTo(study.getJsonData());
		assertThat(cloneInDb.getUserList()).containsOnly(admin);
		assertThat(cloneInDb.getTitle())
				.isEqualTo(study.getTitle() + " (clone)");

		// Not equal
		assertThat(cloneInDb.isLocked()).isFalse();
		assertThat(cloneInDb.getId()).isNotEqualTo(study.getId());
		assertThat(cloneInDb.getId()).isPositive();
		assertThat(cloneInDb.getDirName())
				.isEqualTo(study.getDirName() + "_clone");
		assertThat(cloneInDb.getUuid()).isNotEqualTo(study.getUuid());
		assertThat(cloneInDb.getUuid()).isNotEmpty();

		assertThat(ioUtils.checkStudyAssetsDirExists(cloneInDb.getDirName()))
				.isTrue();

		// Clean-up
		removeStudy(study);
		removeStudy(clone);
	}

	@Test
	public void checkUpdateStudy()
			throws NoSuchAlgorithmException, IOException {
		Study study = importExampleStudy();
		addStudy(study);

		StudyProperties updatedProps = new StudyProperties();
		updatedProps.setDescription("Changed description");
		updatedProps.setComments("Changed comments");
		updatedProps.setJsonData("{}");
		updatedProps.setTitle("Changed Title");
		updatedProps.setUuid("changed uuid");
		long studyId = study.getId();

		studyService.bindToStudyWithoutDirName(study, updatedProps);

		// Changed
		assertThat(study.getTitle()).isEqualTo(updatedProps.getTitle());
		assertThat(study.getDescription())
				.isEqualTo(updatedProps.getDescription());
		assertThat(study.getComments()).isEqualTo(updatedProps.getComments());
		assertThat(study.getJsonData()).isEqualTo(updatedProps.getJsonData());

		// Unchanged
		assertThat(study.getComponentList().size()).isEqualTo(7);
		assertThat(study.getComponent(1).getTitle())
				.isEqualTo("Show JSON input ");
		assertThat(study.getLastComponent().getTitle())
				.isEqualTo("Quit button");
		assertThat(study.getId()).isEqualTo(studyId);
		assertThat(study.getUserList()).contains(admin);
		assertThat(study.getUuid())
				.isEqualTo("5c85bd82-0258-45c6-934a-97ecc1ad6617");

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkExchangeUsers()
			throws NoSuchAlgorithmException, IOException {
		Study study = importExampleStudy();
		addStudy(study);

		User userBla = createAndPersistUser("bla@bla.com", "Bla", "bla");
		createAndPersistUser("blu@blu.com", "Blu", "blu");

		entityManager.getTransaction().begin();
		try {
			String[] userList = { "admin", "bla@bla.com" };
			studyService.exchangeUsers(study, userList);
		} catch (BadRequestException e) {
			Fail.fail();
		}
		entityManager.getTransaction().commit();

		entityManager.getTransaction().begin();
		Study studyInDb = studyDao.findByUuid(study.getUuid());
		assertThat(studyInDb.getUserList()).containsOnly(userBla, admin);
		entityManager.getTransaction().commit();

		// Empty user list
		entityManager.getTransaction().begin();
		try {
			String[] userList = {};
			studyService.exchangeUsers(study, userList);
			Fail.fail();
		} catch (BadRequestException e) {
			assertThat(e.getMessage())
					.isEqualTo(MessagesStrings.STUDY_AT_LEAST_ONE_USER);
		}
		entityManager.getTransaction().commit();

		// Not existent user
		entityManager.getTransaction().begin();
		try {
			String[] userList = { "not_exist", "admin" };
			studyService.exchangeUsers(study, userList);
			Fail.fail();
		} catch (BadRequestException e) {
			assertThat(e.getMessage())
					.isEqualTo(MessagesStrings.userNotExist("not_exist"));
		}
		entityManager.getTransaction().commit();

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void testCheckStudyLocked()
			throws NoSuchAlgorithmException, IOException {
		Study study = importExampleStudy();
		addStudy(study);

		try {
			checker.checkStudyLocked(study);
		} catch (ForbiddenException e) {
			Fail.fail();
		}

		study.setLocked(true);
		try {
			checker.checkStudyLocked(study);
			Fail.fail();
		} catch (ForbiddenException e) {
			assertThat(e.getMessage())
					.isEqualTo(MessagesStrings.studyLocked(study.getId()));
		}

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void testCheckStandardForStudy()
			throws NoSuchAlgorithmException, IOException {
		try {
			checker.checkStandardForStudy(null, 1l, admin);
			Fail.fail();
		} catch (ForbiddenException e) {
			Fail.fail();
		} catch (BadRequestException e) {
			assertThat(e.getMessage())
					.isEqualTo(MessagesStrings.studyNotExist(1l));
		}

		Study study = importExampleStudy();
		addStudy(study);
		try {
			checker.checkStandardForStudy(study, study.getId(), admin);
		} catch (ForbiddenException e) {
			Fail.fail();
		} catch (BadRequestException e) {
			Fail.fail();
		}

		study.getUserList().remove(admin);
		try {
			checker.checkStandardForStudy(study, study.getId(), admin);
			Fail.fail();
		} catch (ForbiddenException e) {
			assertThat(e.getMessage())
					.isEqualTo(MessagesStrings.studyNotUser(admin.getName(),
							admin.getEmail(), study.getId(), study.getTitle()));
		} catch (BadRequestException e) {
			Fail.fail();
		}

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkChangeComponentPosition()
			throws NoSuchAlgorithmException, IOException {
		Study study = importExampleStudy();
		addStudy(study);

		// First component to third position
		Component component = study.getFirstComponent();
		try {
			entityManager.getTransaction().begin();
			studyService.changeComponentPosition("3", study, component);
			entityManager.getTransaction().commit();
		} catch (BadRequestException e) {
			Fail.fail();
		}
		assertThat(study.getComponent(3)).isEqualTo(component);

		// Back to first
		try {
			entityManager.getTransaction().begin();
			studyService.changeComponentPosition("1", study, component);
			entityManager.getTransaction().commit();
		} catch (BadRequestException e) {
			Fail.fail();
		}
		assertThat(study.getComponent(1)).isEqualTo(component);

		// First component to first position -> still first
		try {
			entityManager.getTransaction().begin();
			studyService.changeComponentPosition("1", study, component);
			entityManager.getTransaction().commit();
		} catch (BadRequestException e) {
			Fail.fail();
		}
		assertThat(study.getComponent(1)).isEqualTo(component);

		// Last component to last position -> still last
		component = study.getLastComponent();
		try {
			entityManager.getTransaction().begin();
			studyService.changeComponentPosition("7", study, component);
			entityManager.getTransaction().commit();
		} catch (BadRequestException e) {
			Fail.fail();
		}
		assertThat(study.getLastComponent()).isEqualTo(component);

		// NumberFormatException
		try {
			studyService.changeComponentPosition("bla", study, component);
			Fail.fail();
		} catch (BadRequestException e) {
			assertThat(e.getMessage()).isEqualTo(
					MessagesStrings.COULDNT_CHANGE_POSITION_OF_COMPONENT);
		}

		// IndexOutOfBoundsException
		try {
			studyService.changeComponentPosition("100", study, component);
			Fail.fail();
		} catch (BadRequestException e) {
			assertThat(e.getMessage()).isEqualTo(MessagesStrings
					.studyReorderUnknownPosition("100", study.getId()));
		}

		// Clean-up
		removeStudy(study);
	}

	// @Test
	public void checkRenameStudyAssetsDir()
			throws NoSuchAlgorithmException, IOException {
		Study study = importExampleStudy();
		addStudy(study);

		String oldDirName = study.getDirName();

		entityManager.getTransaction().begin();
		studyService.renameStudyAssetsDir(study, "changed_dirname");
		entityManager.getTransaction().commit();

		assertThat(study.getDirName()).isEqualTo("changed_dirname");
		assertThat(ioUtils.checkStudyAssetsDirExists("changed_dirname"))
				.isTrue();
		assertThat(ioUtils.checkStudyAssetsDirExists(oldDirName)).isFalse();

		// Clean-up
		removeStudy(study);
	}

}
