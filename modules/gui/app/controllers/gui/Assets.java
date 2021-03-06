package controllers.gui;

import javax.inject.Inject;

import play.api.mvc.*;

public class Assets {

	@Inject
	controllers.Assets assets;

	public Action<AnyContent> versioned(String path,
			controllers.Assets.Asset file) {
		return assets.versioned(path, file);
	}
}
