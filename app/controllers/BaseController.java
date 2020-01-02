package controllers;

import play.i18n.MessagesApi;
import play.mvc.Controller;
import util.AppConfig;

import javax.inject.Inject;

public class BaseController extends Controller {

    protected final AppConfig config;
    protected final MessagesApi messagesApi;

    @Inject
    public BaseController(AppConfig config, MessagesApi messagesApi) {
        this.config = config;
        this.messagesApi = messagesApi;
    }
}
