package controllers;

import play.mvc.Controller;
import play.mvc.Result;

/**
 * This controller handles static pages for the slack service
 */
public class HomeController extends Controller {

    public Result success() {
        return ok(views.html.success.render());
    }

    public Result learnMore() {
        return ok(views.html.learn_more.render());
    }
}
