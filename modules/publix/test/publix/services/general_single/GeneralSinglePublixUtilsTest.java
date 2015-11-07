package publix.services.general_single;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import exceptions.publix.ForbiddenPublixException;
import exceptions.publix.PublixException;
import general.Global;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import models.common.Study;
import models.common.workers.GeneralSingleWorker;

import org.fest.assertions.Fail;
import org.junit.Test;

import play.mvc.Http.Cookie;
import publix.services.PublixUtilsTest;
import services.publix.PublixErrorMessages;
import services.publix.general_single.GeneralSingleErrorMessages;
import services.publix.general_single.GeneralSinglePublixUtils;

/**
 * @author Kristian Lange
 */
public class GeneralSinglePublixUtilsTest extends
		PublixUtilsTest<GeneralSingleWorker> {

	private GeneralSingleErrorMessages generalSingleErrorMessages;
	private GeneralSinglePublixUtils generalSinglePublixUtils;

	@Override
	public void before() throws Exception {
		super.before();
		generalSinglePublixUtils = Global.INJECTOR
				.getInstance(GeneralSinglePublixUtils.class);
		publixUtils = generalSinglePublixUtils;
		generalSingleErrorMessages = Global.INJECTOR
				.getInstance(GeneralSingleErrorMessages.class);
		errorMessages = generalSingleErrorMessages;
	}

	@Override
	public void after() throws Exception {
		super.before();
	}

	@Test
	public void checkRetrieveTypedWorker() throws NoSuchAlgorithmException,
			IOException, PublixException {
		GeneralSingleWorker worker = new GeneralSingleWorker();
		addWorker(worker);

		GeneralSingleWorker retrievedWorker = publixUtils
				.retrieveTypedWorker(worker.getId().toString());
		assertThat(retrievedWorker.getId()).isEqualTo(worker.getId());
	}

	@Test
	public void checkRetrieveTypedWorkerWrongType()
			throws NoSuchAlgorithmException, IOException, PublixException {
		try {
			generalSinglePublixUtils.retrieveTypedWorker(admin.getWorker()
					.getId().toString());
			Fail.fail();
		} catch (ForbiddenPublixException e) {
			assertThat(e.getMessage()).isEqualTo(
					generalSingleErrorMessages.workerNotCorrectType(admin
							.getWorker().getId()));
		}
	}

	@Test
	public void checkStudyInCookie() throws NoSuchAlgorithmException,
			IOException, ForbiddenPublixException {
		Study study = importExampleStudy();
		addStudy(study);

		Cookie cookie = mock(Cookie.class);
		// Done studies but not this one
		when(cookie.value()).thenReturn("3,4,5");
		generalSinglePublixUtils.checkStudyInCookie(study, cookie);

		// Null cookie is allowed
		generalSinglePublixUtils.checkStudyInCookie(study, null);

		// Empty cookie value is allowed
		when(cookie.value()).thenReturn("");
		generalSinglePublixUtils.checkStudyInCookie(study, cookie);

		// Weired cookie value is allowed
		when(cookie.value()).thenReturn("foo");
		generalSinglePublixUtils.checkStudyInCookie(study, cookie);

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void checkStudyInCookieAlreadyDone()
			throws NoSuchAlgorithmException, IOException,
			ForbiddenPublixException {
		Study study = importExampleStudy();
		addStudy(study);

		Cookie cookie = mock(Cookie.class);
		// Put this study ID into the cookie
		when(cookie.value()).thenReturn(study.getUuid());

		try {
			generalSinglePublixUtils.checkStudyInCookie(study, cookie);
			Fail.fail();
		} catch (PublixException e) {
			assertThat(e.getMessage()).isEqualTo(
					PublixErrorMessages.STUDY_CAN_BE_DONE_ONLY_ONCE);
		}

		// Clean-up
		removeStudy(study);
	}

	@Test
	public void addStudyToCookie() throws NoSuchAlgorithmException, IOException {
		Study study = importExampleStudy();
		addStudy(study);

		Cookie cookie = mock(Cookie.class);

		// Cookie with two study IDs
		when(cookie.value()).thenReturn("10,20");
		String cookieValue = generalSinglePublixUtils.addStudyToCookie(study,
				cookie);
		assertThat(cookieValue).endsWith("," + study.getUuid());

		// No cookie
		cookieValue = generalSinglePublixUtils.addStudyToCookie(study, null);
		assertThat(cookieValue).isEqualTo(study.getUuid());

		// Clean-up
		removeStudy(study);
	}

}