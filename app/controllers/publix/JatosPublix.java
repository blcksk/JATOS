package controllers.publix;

import models.ComponentModel;
import models.StudyModel;
import models.results.ComponentResult;
import models.results.StudyResult;
import models.workers.JatosWorker;
import play.Logger;
import play.libs.F.Promise;
import play.mvc.Result;
import services.PersistanceUtils;
import controllers.ControllerUtils;
import controllers.Users;
import controllers.routes;
import exceptions.ForbiddenPublixException;
import exceptions.ForbiddenReloadException;
import exceptions.PublixException;

/**
 * Implementation of JATOS' public API for studies and components that are
 * started via JATOS' UI (show study or show component).
 * 
 * @author Kristian Lange
 */
public class JatosPublix extends Publix<JatosWorker> implements IPublix {

	public static final String JATOS_WORKER_ID = "jatosWorkerId";
	public static final String JATOS_SHOW = "jatos_show";
	public static final String SHOW_STUDY = "full_study";
	public static final String SHOW_COMPONENT_START = "single_component_start";
	public static final String SHOW_COMPONENT_FINISHED = "single_component_finished";
	public static final String SHOW_COMPONENT_ID = "show_component_id";

	private static final String CLASS_NAME = JatosPublix.class.getSimpleName();

	protected static final JatosErrorMessages errorMessages = new JatosErrorMessages();
	protected static final JatosPublixUtils utils = new JatosPublixUtils(
			errorMessages);

	public JatosPublix() {
		super(utils);
	}

	@Override
	public Result startStudy(Long studyId) throws PublixException {
		Logger.info(CLASS_NAME + ".startStudy: studyId " + studyId + ", "
				+ "logged-in user's email " + session(Users.COOKIE_EMAIL));
		StudyModel study = utils.retrieveStudy(studyId);

		JatosWorker worker = utils.retrieveUser().getWorker();
		utils.checkWorkerAllowedToStartStudy(worker, study);
		session(WORKER_ID, worker.getId().toString());

		Long componentId = null;
		String jatosShow = utils.retrieveJatosShowCookie();
		switch (jatosShow) {
		case SHOW_STUDY:
			componentId = utils.retrieveFirstActiveComponent(study).getId();
			break;
		case SHOW_COMPONENT_START:
			componentId = Long.valueOf(session(SHOW_COMPONENT_ID));
			session().remove(SHOW_COMPONENT_ID);
			break;
		case SHOW_COMPONENT_FINISHED:
			throw new ForbiddenPublixException(
					PublixErrorMessages.STUDY_NEVER_STARTED_FROM_JATOS);
		}
		utils.finishAllPriorStudyResults(worker, study);
		PersistanceUtils.createStudyResult(study, worker);
		return redirect(controllers.publix.routes.PublixInterceptor
				.startComponent(studyId, componentId));
	}

	@Override
	public Promise<Result> startComponent(Long studyId, Long componentId)
			throws PublixException {
		Logger.info(CLASS_NAME + ".startComponent: studyId " + studyId + ", "
				+ "componentId " + componentId + ", "
				+ "logged-in user's email " + session(Users.COOKIE_EMAIL));
		StudyModel study = utils.retrieveStudy(studyId);
		JatosWorker worker = utils.retrieveTypedWorker(session(WORKER_ID));
		ComponentModel component = utils.retrieveComponent(study, componentId);
		utils.checkWorkerAllowedToDoStudy(worker, study);
		utils.checkComponentBelongsToStudy(study, component);

		// Check if it's a single component show or a whole study show
		String jatosShow = utils.retrieveJatosShowCookie();
		StudyResult studyResult = utils.retrieveWorkersLastStudyResult(worker,
				study);
		switch (jatosShow) {
		case SHOW_STUDY:
			break;
		case SHOW_COMPONENT_START:
			session(JatosPublix.JATOS_SHOW, JatosPublix.SHOW_COMPONENT_FINISHED);
			break;
		case SHOW_COMPONENT_FINISHED:
			ComponentResult lastComponentResult = utils
					.retrieveLastComponentResult(studyResult);
			if (!lastComponentResult.getComponent().equals(component)) {
				// It's already the second component (first is finished and it
				// isn't a reload of the same one). Finish study after first
				// component.
				return Promise
						.pure((Result) redirect(controllers.publix.routes.PublixInterceptor
								.finishStudy(studyId, true, null)));
			}
			break;
		}

		ComponentResult componentResult = null;
		try {
			componentResult = utils.startComponent(component, studyResult);
		} catch (ForbiddenReloadException e) {
			return Promise
					.pure((Result) redirect(controllers.publix.routes.PublixInterceptor
							.finishStudy(studyId, false, e.getMessage())));
		}
		PublixUtils.setIdCookie(studyResult, componentResult, worker);
		String urlPath = StudiesAssets.getComponentUrlPath(study.getDirName(),
				component);
		String urlWithQueryStr = StudiesAssets
				.getUrlWithRequestQueryString(urlPath);
		return forwardTo(urlWithQueryStr);
	}

	@Override
	public Result startNextComponent(Long studyId) throws PublixException {
		Logger.info(CLASS_NAME + ".startNextComponent: studyId " + studyId
				+ ", " + "logged-in user's email "
				+ session(Users.COOKIE_EMAIL));
		StudyModel study = utils.retrieveStudy(studyId);
		JatosWorker worker = utils.retrieveTypedWorker(session(WORKER_ID));
		utils.checkWorkerAllowedToDoStudy(worker, study);

		StudyResult studyResult = utils.retrieveWorkersLastStudyResult(worker,
				study);

		// Check if it's a single component show or a whole study show
		String jatosShow = utils.retrieveJatosShowCookie();
		switch (jatosShow) {
		case SHOW_STUDY:
			studyResult = utils.retrieveWorkersLastStudyResult(worker, study);
			break;
		case SHOW_COMPONENT_START:
			// Should never happen
			session(JatosPublix.JATOS_SHOW, JatosPublix.SHOW_COMPONENT_FINISHED);
			return redirect(controllers.publix.routes.PublixInterceptor
					.finishStudy(studyId, false, null));
		case SHOW_COMPONENT_FINISHED:
			// It's already the second component (first is finished). Finish
			// study after first component.
			return redirect(controllers.publix.routes.PublixInterceptor
					.finishStudy(studyId, true, null));
		}

		ComponentModel nextComponent = utils
				.retrieveNextActiveComponent(studyResult);
		if (nextComponent == null) {
			// Study has no more components -> finish it
			return redirect(controllers.publix.routes.PublixInterceptor
					.finishStudy(studyId, true, null));
		}
		String urlWithQueryString = StudiesAssets
				.getUrlWithRequestQueryString(controllers.publix.routes.PublixInterceptor
						.startComponent(studyId, nextComponent.getId()).url());
		return redirect(urlWithQueryString);
	}

	@Override
	public Result abortStudy(Long studyId, String message)
			throws PublixException {
		Logger.info(CLASS_NAME + ".abortStudy: studyId " + studyId + ", "
				+ "logged-in user email " + session(Users.COOKIE_EMAIL) + ", "
				+ "message \"" + message + "\"");
		StudyModel study = utils.retrieveStudy(studyId);
		JatosWorker worker = utils.retrieveTypedWorker(session(WORKER_ID));
		utils.checkWorkerAllowedToDoStudy(worker, study);

		StudyResult studyResult = utils.retrieveWorkersLastStudyResult(worker,
				study);
		if (!utils.studyDone(studyResult)) {
			utils.abortStudy(message, studyResult);
			Publix.session().remove(JatosPublix.JATOS_SHOW);
		}

		PublixUtils.discardIdCookie();
		if (ControllerUtils.isAjax()) {
			return ok();
		} else {
			return redirect(routes.Studies.index(study.getId(), message));
		}
	}

	@Override
	public Result finishStudy(Long studyId, Boolean successful, String errorMsg)
			throws PublixException {
		Logger.info(CLASS_NAME + ".finishStudy: studyId " + studyId + ", "
				+ "logged-in user email " + session(Users.COOKIE_EMAIL) + ", "
				+ "successful " + successful + ", " + "errorMsg \"" + errorMsg
				+ "\"");
		StudyModel study = utils.retrieveStudy(studyId);
		JatosWorker worker = utils.retrieveTypedWorker(session(WORKER_ID));
		utils.checkWorkerAllowedToDoStudy(worker, study);

		StudyResult studyResult = utils.retrieveWorkersLastStudyResult(worker,
				study);
		if (!utils.studyDone(studyResult)) {
			utils.finishStudy(successful, errorMsg, studyResult);
			Publix.session().remove(JatosPublix.JATOS_SHOW);
		}
		
		PublixUtils.discardIdCookie();
		if (ControllerUtils.isAjax()) {
			return ok(errorMsg);
		} else {
			return redirect(routes.Studies.index(study.getId(), errorMsg));
		}
	}

}
