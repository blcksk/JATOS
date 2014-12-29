package controllers.publix;

import models.StudyModel;
import models.results.StudyResult;
import models.workers.MTSandboxWorker;
import models.workers.MTWorker;
import models.workers.Worker;
import exceptions.ForbiddenPublixException;
import exceptions.PublixException;

/**
 * Special PublixUtils for MTPublix (studies started via MTurk).
 * 
 * @author Kristian Lange
 */
public class MTPublixUtils extends PublixUtils<MTWorker> {

	private MTErrorMessages errorMessages;

	public MTPublixUtils(MTErrorMessages errorMessages) {
		super(errorMessages);
		this.errorMessages = errorMessages;
	}

	@Override
	public MTWorker retrieveTypedWorker(String workerIdStr)
			throws PublixException {
		Worker worker = retrieveWorker(workerIdStr);
		if (!(worker instanceof MTWorker)) {
			throw new ForbiddenPublixException(
					errorMessages.workerNotCorrectType(worker.getId()));
		}
		return (MTWorker) worker;
	}
	
	@Override
	public void checkWorkerAllowedToStartStudy(MTWorker worker, StudyModel study)
			throws ForbiddenPublixException {
		if (!(worker instanceof MTSandboxWorker)) {
			for (StudyResult studyResult : worker.getStudyResultList()) {
				if (studyResult.getStudy().getId() == study.getId()) {
					throw new ForbiddenPublixException(
							PublixErrorMessages.STUDY_CAN_BE_DONE_ONLY_ONCE);
				}
			}
		}
		checkWorkerAllowedToDoStudy(worker, study);
	}

	@Override
	public void checkWorkerAllowedToDoStudy(MTWorker worker, StudyModel study)
			throws ForbiddenPublixException {
		// Sandbox workers can repeat studies
		if (worker instanceof MTSandboxWorker) {
			return;
		}
		// MTurk workers can't repeat studies
		if (didStudyAlready(worker, study)) {
			throw new ForbiddenPublixException(
					PublixErrorMessages.STUDY_CAN_BE_DONE_ONLY_ONCE);
		}
	}

}
