package controllers.publix;

import play.Logger;
import play.libs.F;
import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import utils.common.ControllerUtils;
import exceptions.publix.InternalServerErrorPublixException;
import exceptions.publix.PublixException;

/**
 * For all actions in a controller that is annotated with PublixAction catch
 * {@link PublixException} and return a Result generated by the exception.
 * 
 * @author Kristian Lange
 */
public class PublixAction extends play.mvc.Action.Simple {

	public F.Promise<Result> call(Http.Context ctx) throws Throwable {
		Promise<Result> call;
		try {
			call = delegate.call(ctx);
		} catch (InternalServerErrorPublixException e) {
			Result result = e.getSimpleResult();
			// Log exception with stack trace
			Logger.info("PublixException during call "
					+ Controller.request().uri() + ": " + e.getMessage(), e);
			call = Promise.pure(result);
		} catch (PublixException e) {
			Result result = e.getSimpleResult();
			// Log exception without stack trace
			Logger.info("PublixException during call "
					+ Controller.request().uri() + ": " + e.getMessage());
			call = Promise.pure(result);
		} catch (Exception e) {
			// Log exception with stack trace
			Logger.error("Exception during call " + Controller.request().uri()
					+ ": " + e.getMessage(), e);
			if (ControllerUtils.isAjax()) {
				call = Promise
						.<Result> pure(internalServerError(e.getMessage()));
			} else {
				call = Promise.<Result> pure(
						internalServerError(views.html.publix.error
								.render("Internal JATOS error during "
										+ Controller.request().uri() + ": "
										+ e.getMessage())));
			}
		}
		return call;
	}

}
