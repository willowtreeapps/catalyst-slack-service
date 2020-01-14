package controllers;

import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSBodyWritables;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AppService;
import util.AppConfig;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthController extends Controller implements WSBodyReadables, WSBodyWritables {

    private AppService _service;
    private final AppConfig _config;
    private final MessagesApi _messagesApi;

    @Inject
    public AuthController(AppService service, AppConfig config, MessagesApi messagesApi) {
        this._service = service;
        this._config = config;
        this._messagesApi = messagesApi;
    }

    /**
     * Handle all oauth requests from Slack.
     */
    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var requestCode = httpRequest.queryString("code");
        // send a get request to Slack with the code to get token for authed user

        //TODO check optional requestCode
        return _service.getAuth(requestCode.get()).thenComposeAsync( response -> {
            if (response.teamId != null && response.userId != null && response.userToken != null) {
    System.out.println("adding to db: " +response.teamId+"_"+response.userId+" : " + response.userToken);
                return CompletableFuture.completedFuture(ok(Json.toJson(Map.of("ok", response.ok, "redirectTo", "authorized page"))));
                //TODO: add to tokens
//                DbManager.getInstance().addTeamToken(teamId, userToken);
//                DbManager.getInstance().addUserToken(teamId, userId, userToken);
            } else {
                return CompletableFuture.completedFuture(badRequest(Json.toJson(Map.of("ok", response.ok, "error", response.error))));
            }
        });
    }

    public Result signin() {
        // TODO: is this the right URL?
        return found(_config.getAppOauthUrl());
    }
}
