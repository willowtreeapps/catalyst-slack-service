package controllers;

import com.typesafe.config.Config;
import play.mvc.Result;

import javax.inject.Inject;

/**
 * This controller handles static pages for the slack service
 */
public class HomeController extends BaseController {

    @Inject
    public HomeController(Config config) {
        super(config);
    }

    public Result success() {
        return ok(views.html.success.render());
    }

    public Result learnMore() {
        return ok(views.html.learn_more.render());
    }
}
