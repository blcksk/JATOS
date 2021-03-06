package services.publix.workers;

import javax.inject.Inject;
import javax.inject.Singleton;

import controllers.publix.Publix;
import controllers.publix.workers.JatosPublix;
import daos.common.BatchDao;
import daos.common.ComponentDao;
import daos.common.ComponentResultDao;
import daos.common.StudyDao;
import daos.common.StudyResultDao;
import daos.common.UserDao;
import daos.common.worker.WorkerDao;
import exceptions.publix.ForbiddenPublixException;
import models.common.User;
import models.common.workers.JatosWorker;
import models.common.workers.Worker;
import services.publix.PublixUtils;
import services.publix.ResultCreator;

/**
 * JatosPublix' implementation of PublixUtils (studies or components started via
 * JATOS' UI).
 * 
 * @author Kristian Lange
 */
@Singleton
public class JatosPublixUtils extends PublixUtils<JatosWorker> {

	private final JatosErrorMessages errorMessages;
	private final UserDao userDao;

	@Inject
	JatosPublixUtils(ResultCreator resultCreator,
			JatosErrorMessages errorMessages, UserDao userDao,
			StudyDao studyDao, StudyResultDao studyResultDao,
			ComponentDao componentDao, ComponentResultDao componentResultDao,
			WorkerDao workerDao, BatchDao batchDao) {
		super(resultCreator, errorMessages, studyDao, studyResultDao,
				componentDao, componentResultDao, workerDao, batchDao);
		this.errorMessages = errorMessages;
		this.userDao = userDao;
	}

	@Override
	public JatosWorker retrieveTypedWorker(String workerIdStr)
			throws ForbiddenPublixException {
		Worker worker = retrieveWorker(workerIdStr);
		if (!(worker instanceof JatosWorker)) {
			throw new ForbiddenPublixException(
					errorMessages.workerNotCorrectType(worker.getId()));
		}
		return (JatosWorker) worker;
	}

	/**
	 * Retrieves the currently logged-in user or throws an
	 * ForbiddenPublixException if none is logged-in.
	 */
	public User retrieveLoggedInUser() throws ForbiddenPublixException {
		String email = Publix.session(JatosPublix.SESSION_EMAIL);
		if (email == null) {
			throw new ForbiddenPublixException(
					JatosErrorMessages.NO_USER_LOGGED_IN);
		}
		User loggedInUser = userDao.findByEmail(email);
		if (loggedInUser == null) {
			throw new ForbiddenPublixException(
					errorMessages.userNotExist(email));
		}
		return loggedInUser;
	}

	/**
	 * Retrieves the kind of jatos run, whole study or single component, this
	 * is. This information was stored in the session in the prior action.
	 */
	public String retrieveJatosShowFromSession()
			throws ForbiddenPublixException {
		String jatosShow = Publix.session(JatosPublix.JATOS_RUN);
		if (jatosShow == null) {
			throw new ForbiddenPublixException(
					JatosErrorMessages.STUDY_OR_COMPONENT_NEVER_STARTED_FROM_JATOS);
		}
		return jatosShow;
	}

}
