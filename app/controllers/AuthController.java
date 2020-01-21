package controllers;

import db.TokenHandler;
import db.TokenKey;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AppService;
import util.AppConfig;
import util.MessageHandler;
import util.ResultHelper;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthController extends Controller {

    private AppService _service;
    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final TokenHandler _tokenDb;

    @Inject
    public AuthController(AppService service, AppConfig config, MessagesApi messagesApi, TokenHandler db) {
        this._service = service;
        this._config = config;
        this._messagesApi = messagesApi;
        this._tokenDb = db;
    }

    /**
     * Handle all oauth requests from Slack.
     */
    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var requestCode = httpRequest.queryString("code");

        if (requestCode.isEmpty()) {
            var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_CODE);
        }

        // send a get request to Slack with the code to get token for authed user
        return _service.getAuthorization(requestCode.get()).thenComposeAsync(response -> {
            if (response.error != null || response.teamId == null || response.userId == null || response.userToken == null) {
                return CompletableFuture.completedFuture(badRequest(Json.toJson(Map.of(
                        "ok", response.ok,
                        "error", response.error))));
            }

            var dbKey = new TokenKey();
            dbKey.teamId = response.teamId;
            dbKey.userId = response.userId;
            _tokenDb.setUserToken(dbKey, response.userToken);
            //TODO: return found(APP_INSTALL_URL)
            return CompletableFuture.completedFuture(found(_config.getAuthorizedUrl()));
        });
    }

    public Result signin() {
        return found(_config.getAppSigninUrl());
    }
}
