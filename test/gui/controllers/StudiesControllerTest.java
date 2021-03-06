package gui.controllers;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static play.mvc.Http.Status.OK;
import static play.mvc.Http.Status.SEE_OTHER;
import static play.test.Helpers.contentAsString;
import static play.test.Helpers.route;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;

import controllers.gui.Users;
import general.AbstractTest;
import general.common.MessagesStrings;
import models.common.Study;
import models.gui.StudyProperties;
import play.mvc.Http.RequestBuilder;
import play.mvc.Result;
import services.gui.BreadcrumbsService;
import utils.common.JsonUtils;

/**
 * Testing actions of controller.Studies.
 * 
 * @author Kristian Lange
 */
public class StudiesControllerTest extends AbstractTest {

	private static Study studyTemplate;

	@Override
	public void before() throws Exception {
		studyTemplate = importExampleStudy();
	}

	@Override
	public void after() throws Exception {
		ioUtils.removeStudyAssetsDir(studyTemplate.getDirName());
	}

	@Test
	public void callIndex() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		RequestBuilder request = new RequestBuilder().method("GET")
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies.study(studyClone.getId())
						.url());
		Result result = route(request);
		assertThat(result.status()).isEqualTo(OK);
		assertThat(result.charset()).isEqualTo("utf-8");
		assertThat(result.contentType()).isEqualTo("text/html");
		assertThat(contentAsString(result)).contains("Components");

		// Clean up
		removeStudy(studyClone);
	}

	@Test
	public void callSubmitCreated() throws Exception {
		Map<String, String> formMap = new HashMap<String, String>();
		formMap.put(StudyProperties.TITLE, "Title Test");
		formMap.put(StudyProperties.DESCRIPTION, "Description test.");
		formMap.put(StudyProperties.COMMENTS, "Comments test.");
		formMap.put(StudyProperties.DIRNAME, "dirName_submit");
		formMap.put(StudyProperties.JSON_DATA, "{}");
		RequestBuilder request = new RequestBuilder().method("POST")
				.bodyForm(formMap)
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies.submitCreated().url());
		Result result = route(request);

		assertEquals(OK, result.status());

		// Get study ID of created study from response's header
		assertThat(contentAsString(result)).isNotEmpty();
		Long studyId = Long.valueOf(contentAsString(result));

		Study study = studyDao.findById(studyId);
		assertEquals("Title Test", study.getTitle());
		assertEquals("Description test.", study.getDescription());
		assertEquals("dirName_submit", study.getDirName());
		assertEquals("{}", study.getJsonData());
		assertThat((study.getComponentList().isEmpty()));
		assertThat((study.getUserList().contains(admin)));
		assertThat((!study.isLocked()));

		// Clean up
		removeStudy(study);
	}

	@Test
	public void callSubmitCreatedValidationError()
			throws JsonProcessingException, IOException {
		// Fill with non-valid values
		Map<String, String> formMap = new HashMap<String, String>();
		formMap.put(StudyProperties.TITLE, " ");
		formMap.put(StudyProperties.DESCRIPTION, "Description test <b>.");
		formMap.put(StudyProperties.COMMENTS, "Comments test <i>.");
		formMap.put(StudyProperties.DIRNAME, "%.test");
		formMap.put(StudyProperties.JSON_DATA, "{");
		RequestBuilder request = new RequestBuilder().method("POST")
				.bodyForm(formMap)
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies.submitCreated().url());
		Result result = route(request);

		assertThat(result.contentType()).isEqualTo("application/json");
		JsonNode node = JsonUtils.OBJECTMAPPER
				.readTree(contentAsString(result));
		assertThat(node.get(StudyProperties.TITLE).toString())
				.isEqualTo("[\"" + MessagesStrings.MISSING_TITLE + "\"]");
		assertThat(node.get(StudyProperties.DESCRIPTION).toString())
				.isEqualTo("[\"" + MessagesStrings.NO_HTML_ALLOWED + "\"]");
		assertThat(node.get(StudyProperties.COMMENTS).toString())
				.isEqualTo("[\"" + MessagesStrings.NO_HTML_ALLOWED + "\"]");
		assertThat(node.get(StudyProperties.DIRNAME).toString())
				.isEqualTo("[\"" + MessagesStrings.INVALID_DIR_NAME + "\"]");
		assertThat(node.get(StudyProperties.JSON_DATA).toString())
				.isEqualTo("[\"" + MessagesStrings.INVALID_JSON_FORMAT + "\"]");
	}

	@Test
	public void callSubmitCreatedStudyAssetsDirExists() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);
		Map<String, String> formMap = new HashMap<String, String>();
		formMap.put(StudyProperties.TITLE, "Title Test");
		formMap.put(StudyProperties.DESCRIPTION, "Description test.");
		formMap.put(StudyProperties.COMMENTS, "Comments test.");
		formMap.put(StudyProperties.DIRNAME, studyClone.getDirName());
		formMap.put(StudyProperties.JSON_DATA, "{}");
		RequestBuilder request = new RequestBuilder().method("POST")
				.bodyForm(formMap)
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies.submitCreated().url());
		Result result = route(request);

		assertThat(contentAsString(result)).contains(
				"{\"dirName\":[\"Study assets' directory (basic_example_study_clone) couldn't be created because it already exists.\"]}");

		// Cleanup
		removeStudy(studyClone);
	}

	@Test
	public void callProperties() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		RequestBuilder request = new RequestBuilder().method("GET")
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies
						.properties(studyClone.getId()).url());
		Result result = route(request);

		assertThat(result.status()).isEqualTo(OK);
		assertThat(result.charset()).isEqualTo("utf-8");
		assertThat(result.contentType()).isEqualTo("application/json");

		// Check properties in JSON
		JsonNode node = JsonUtils.OBJECTMAPPER
				.readTree(contentAsString(result));
		assertThat(node.get(StudyProperties.TITLE).toString())
				.isEqualTo("\"" + studyClone.getTitle() + "\"");
		assertThat(node.get(StudyProperties.COMMENTS).toString())
				.isEqualTo("null");
		assertThat(node.get(StudyProperties.DESCRIPTION).toString())
				.isEqualTo("\"" + studyClone.getDescription() + "\"");
		assertThat(node.get(StudyProperties.DIRNAME).toString())
				.isEqualTo("\"" + studyClone.getDirName() + "\"");
		assertThat(node.get(StudyProperties.UUID).toString())
				.isEqualTo("\"" + studyClone.getUuid() + "\"");
		assertThat(node.get(StudyProperties.JSON_DATA).toString())
				.isEqualTo("\"{\\\"totalStudySlides\\\":17}\"");
		assertThat(node.get(StudyProperties.LOCKED).toString())
				.isEqualTo(String.valueOf(studyClone.isLocked()));
		assertThat(node.get(StudyProperties.GROUP_STUDY).toString())
				.isEqualTo(String.valueOf(studyClone.isGroupStudy()));

		// Clean up
		removeStudy(studyClone);
	}

	@Test
	public void callSubmitEdited() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		Map<String, String> formMap = new HashMap<String, String>();
		formMap.put(StudyProperties.TITLE, "Title Test");
		formMap.put(StudyProperties.DESCRIPTION, "Description test.");
		formMap.put(StudyProperties.COMMENTS, "Comments test.");
		formMap.put(StudyProperties.DIRNAME, "dirName_submitEdited");
		formMap.put(StudyProperties.JSON_DATA, "{}");
		RequestBuilder request = new RequestBuilder().method("POST")
				.bodyForm(formMap)
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies
						.submitEdited(studyClone.getId()).url());
		Result result = route(request);

		assertEquals(OK, result.status());

		// TODO It would be nice to test the edited study here (somehow can't
		// find edited study in DB)
		// Study study = studyDao.findById(studyClone.getId());

		// Clean up
		// Weird: Can't get the edited study from DB but the edited assets dir
		// is there
		ioUtils.removeStudyAssetsDir("dirName_submitEdited");
		removeStudy(studyClone);
	}

	@Test
	public void callSwapLock() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);
		RequestBuilder request = new RequestBuilder().method("POST")
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies
						.toggleLock(studyClone.getId()).url());
		Result result = route(request);

		assertThat(result.status()).isEqualTo(OK);
		assertThat(contentAsString(result)).contains("true");

		// Clean up
		removeStudy(studyClone);
	}

	@Test
	public void callRemove() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		RequestBuilder request = new RequestBuilder().method("DELETE")
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies.remove(studyClone.getId())
						.url());
		Result result = route(request);
		assertThat(result.status()).isEqualTo(OK);

		// Clean up not necessary since we call remove action
	}

	@Test
	public void callCloneStudy() throws Exception {
		Study study = cloneAndPersistStudy(studyTemplate);

		RequestBuilder request = new RequestBuilder().method("GET")
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies.cloneStudy(study.getId())
						.url());
		Result result = route(request);

		assertThat(result.status()).isEqualTo(OK);

		// Clean up
		ioUtils.removeStudyAssetsDir(study.getDirName() + "_clone");
		removeStudy(study);
	}

	@Test
	public void callChangeUser() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		RequestBuilder request = new RequestBuilder().method("GET")
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies
						.submitChangedUsers(studyClone.getId()).url());
		Result result = route(request);

		assertThat(result.status()).isEqualTo(OK);

		// Clean up
		removeStudy(studyClone);
	}

	@Test
	public void callSubmitChangedUsers() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		RequestBuilder request = new RequestBuilder().method("POST")
				.bodyForm(ImmutableMap.of(Study.USERS, "admin"))
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies
						.submitChangedUsers(studyClone.getId()).url());
		Result result = route(request);

		assertEquals(OK, result.status());

		// Clean up
		removeStudy(studyClone);
	}

	@Test
	public void callSubmitChangedUsersZeroUsers() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		RequestBuilder request = new RequestBuilder().method("POST")
				.bodyForm(ImmutableMap.of("bla", "blu"))
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies
						.submitChangedUsers(studyClone.getId()).url());
		Result result = route(request);

		assertThat(contentAsString(result))
				.contains("An study should have at least one user.");

		// Clean up
		removeStudy(studyClone);
	}

	@Test
	public void callChangeComponentOrder() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		// Move first component to second position
		RequestBuilder request = new RequestBuilder().method("POST")
				.bodyForm(ImmutableMap.of(Study.USERS, "admin"))
				.session(Users.SESSION_EMAIL,
						admin.getEmail())
				.uri(controllers.gui.routes.Studies
						.changeComponentOrder(studyClone.getId(),
								studyClone.getComponentList().get(0).getId(),
								"2")
						.url());
		Result result = route(request);

		assertThat(result.status()).isEqualTo(OK);

		// Move second component to first position
		request = new RequestBuilder().method("POST")
				.bodyForm(ImmutableMap.of(Study.USERS, "admin"))
				.session(Users.SESSION_EMAIL,
						admin.getEmail())
				.uri(controllers.gui.routes.Studies
						.changeComponentOrder(studyClone.getId(),
								studyClone.getComponentList().get(1).getId(),
								"1")
						.url());
		result = route(request);

		assertThat(result.status()).isEqualTo(OK);

		// Clean up
		removeStudy(studyClone);
	}

	@Test
	public void callShowStudy() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		RequestBuilder request = new RequestBuilder().method("GET")
				.bodyForm(ImmutableMap.of(Study.USERS, "admin"))
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies
						.runStudy(studyClone.getId(), -1l).url());
		Result result = route(request);

		assertEquals(SEE_OTHER, result.status());

		// Clean up
		removeStudy(studyClone);
	}

	@Test
	public void callWorkers() throws Exception {
		Study studyClone = cloneAndPersistStudy(studyTemplate);

		RequestBuilder request = new RequestBuilder().method("GET")
				.session(Users.SESSION_EMAIL, admin.getEmail())
				.uri(controllers.gui.routes.Studies.workers(studyClone.getId())
						.url());
		Result result = route(request);

		assertThat(result.status()).isEqualTo(OK);
		assertThat(contentAsString(result))
				.contains(BreadcrumbsService.WORKER_SETUP);

		// Clean up
		removeStudy(studyClone);
	}

}
