package controllers;

import com.typesafe.config.Config;
import play.i18n.MessagesApi;
import play.mvc.Result;

import javax.inject.Inject;

/**
 * This controller handles static pages for the slack service
 */
public class HomeController extends BaseController {

    @Inject
    public HomeController(Config config, MessagesApi messagesApi) {
        super(config, messagesApi);
    }

    public Result success() {
        return ok(views.html.success.render());
    }

    public Result learnMore() {
        return ok(views.html.learn_more.render());
    }
}
