package controllers;

import db.DbManager;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AppService;
import util.AppConfig;
import util.MessageHandler;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthController extends Controller {

    private AppService _service;
    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final DbManager _db;

    @Inject
    public AuthController(AppService service, AppConfig config, MessagesApi messagesApi, DbManager db) {
        this._service = service;
        this._config = config;
        this._messagesApi = messagesApi;
        this._db = db;
    }

    /**
     * Handle all oauth requests from Slack.
     */
    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var requestCode = httpRequest.queryString("code");

        if (requestCode.isEmpty()) {
            var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
            return CompletableFuture.completedFuture(badRequest( Json.toJson(Map.of(
                    "ok", Boolean.valueOf(false),
                    "error", messages.get("error.missing.code")))));
        }

        // send a get request to Slack with the code to get token for authed user
        return _service.getAuth(requestCode.get()).thenComposeAsync( response -> {
            if (response.error != null || response.teamId == null || response.userId == null || response.userToken == null) {
                return CompletableFuture.completedFuture(badRequest(Json.toJson(Map.of(
                        "ok", response.ok,
                        "error", response.error))));
            }

            _db.addUserToken(response.teamId, response.userId, response.userToken);
            //TODO: add to team tokens??
            //TODO: return found(APP_INSTALL_URL)
            return CompletableFuture.completedFuture(ok(Json.toJson(Map.of(
                    "ok", response.ok,
                    "redirect", "authorized page"))));
        });
    }

    public Result signin() {
        // TODO: verify if this is the right url
        return found(_config.getAppSigninUrl());
    }
}
