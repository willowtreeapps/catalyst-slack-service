package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.db.AnalyticsHandler;
import org.catalyst.slackservice.db.Bot;
import org.catalyst.slackservice.db.TokenHandler;
import org.catalyst.slackservice.db.TokenKey;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.util.AppConfig;
import org.catalyst.slackservice.util.MessageHandler;
import org.catalyst.slackservice.util.ResultHelper;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AuthController extends Controller {

    private AppService _service;
    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final TokenHandler _tokenDb;
    private final AnalyticsHandler _analyticsDb;
    private static final String BOT_TOKEN_TYPE = "bot";
    private static final String ERROR_ACCESS_DENIED = "access_denied";

    @Inject
    public AuthController(AppService service, AppConfig config, MessagesApi messagesApi, TokenHandler db, AnalyticsHandler analyticsDb) {
        this._service = service;
        this._config = config;
        this._messagesApi = messagesApi;
        this._tokenDb = db;
        this._analyticsDb = analyticsDb;
    }

    /**
     * Handle all oauth requests from Slack.
     */
    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var requestCode = httpRequest.queryString("code");
        var error = httpRequest.queryString("error");

        if (requestCode.isEmpty() && error.isEmpty()) {
            var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_CODE);
        }

        if (error.isPresent()) {
            var errorValue = error.get();
            if (ERROR_ACCESS_DENIED.equals(errorValue)) {
                return CompletableFuture.completedFuture(found(_config.getLearnMoreUrl()));
            }
            return ResultHelper.badRequest(errorValue);
        }

        // send a get request to Slack with the code to get token for authed user
        return _service.getAuthorization(requestCode.get()).thenComposeAsync(response -> {
            var teamId = response.team != null ? response.team.id : null;
            var userId = response.user != null ? response.user.id : null;
            var token = response.user != null ? response.user.accessToken : null;
            if (response.error != null || teamId == null || userId == null || token == null) {
                LoggerFactory.getLogger(AuthController.class).error(
                    "get authorization failed. teamId: {}, userId: {}, null token?: {}, error: {}", teamId, userId, (token == null), response.error);

                return CompletableFuture.completedFuture(badRequest(Json.toJson(Map.of(
                        "ok", response.ok,
                        "error", response.error))));
            }

            var teamName = response.team != null ? response.team.name : null;
            if (teamName != null) {
                _analyticsDb.setTeamName(teamId, teamName);
            }

            if (BOT_TOKEN_TYPE.equals(response.tokenType) && response.botUserId != null && response.accessToken != null) {
                var bot = new Bot();
                bot.token = response.accessToken;
                bot.userId = response.botUserId;
                _tokenDb.setBotInfo(teamId, bot);
            }

            var dbKey = new TokenKey();
            dbKey.teamId = teamId;
            dbKey.userId = userId;
            _tokenDb.setUserToken(dbKey, token);

            return CompletableFuture.completedFuture(found(_config.getAuthorizedUrl()));
        });
    }

    public Result signin() {
        return found(_config.getAppSigninUrl());
    }
}
