package controllers.gui;

import javax.inject.Inject;
import javax.inject.Singleton;

import controllers.gui.actionannotations.JatosGuiAction.JatosGui;
import daos.common.UserDao;
import general.common.MessagesStrings;
import general.gui.FlashScopeMessaging;
import play.Logger;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import utils.common.HashUtils;

/**
 * Controller that deals with login/logout.
 * 
 * @author Kristian Lange
 */
@JatosGui
@Singleton
public class Authentication extends Controller {

	private static final String CLASS_NAME = Authentication.class
			.getSimpleName();

	public static final String LOGGED_IN_USER = "loggedInUser";

	private final UserDao userDao;

	@Inject
	Authentication(UserDao userDao) {
		this.userDao = userDao;
	}

	/**
	 * Shows the login form view.
	 */
	public Result login() {
		Logger.info(CLASS_NAME + ".login");
		return ok(views.html.gui.auth.login
				.render(Form.form(Authentication.Login.class)));
	}

	/**
	 * Deals with login form post.
	 */
	@Transactional
	public Result authenticate() {
		Form<Login> loginForm = Form.form(Login.class).bindFromRequest();
		String email = loginForm.data().get("email");
		String password = loginForm.data().get("password");
		String passwordHash = HashUtils.getHashMDFive(password);
		if (userDao.authenticate(email, passwordHash)) {
			session(Users.SESSION_EMAIL, email);
			return redirect(controllers.gui.routes.Home.home());
		} else {
			loginForm.reject("Invalid user or password");
			return badRequest(views.html.gui.auth.login.render(loginForm));
		}
	}

	/**
	 * Shows login view with an logout message.
	 */
	public Result logout() {
		Logger.info(CLASS_NAME + ".logout: " + session(Users.SESSION_EMAIL));
		session().remove(Users.SESSION_EMAIL);
		FlashScopeMessaging.success(MessagesStrings.YOUVE_BEEN_LOGGED_OUT);
		return redirect(controllers.gui.routes.Authentication.login());
	}

	/**
	 * Inner class needed for login template
	 */
	public static class Login {
		public String email;
		public String password;
	}

}
