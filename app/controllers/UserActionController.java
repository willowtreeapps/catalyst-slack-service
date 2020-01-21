package controllers;

import db.AnalyticsHandler;
import db.AnalyticsKey;
import db.TokenHandler;
import db.TokenKey;
import domain.InteractiveMessage;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AppService;
import services.MessageCorrector;
import util.AppConfig;
import util.MessageHandler;
import util.RequestVerifier;

import javax.inject.Inject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UserActionController extends Controller {

    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final MessageCorrector _biasCorrector;
    private final AppService _slackService;
    private final HttpExecutionContext _ec;
    private final TokenHandler _tokenDb;
    private final AnalyticsHandler _analyticsDb;

    @Inject
    public UserActionController(HttpExecutionContext ec, AppConfig config, MessagesApi messagesApi,
                                MessageCorrector biasCorrector, AppService slackService, TokenHandler tokenDb, AnalyticsHandler analyticsDb) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._biasCorrector = biasCorrector;
        this._slackService = slackService;
        this._ec = ec;
        this._tokenDb = tokenDb;
        this._analyticsDb = analyticsDb;
    }

    private static CompletionStage<Result> resultBadRequest(MessageHandler messages, String error) {
        return CompletableFuture.completedFuture(badRequest(messages.error(error)));
    }

    private static CompletionStage<Result> resultNoContent() {
        return CompletableFuture.completedFuture(noContent());
    }

    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var body = httpRequest.body().asFormUrlEncoded();

        if (body == null) {
            return resultNoContent();
        }

        var payload = body.get("payload");
        if (payload == null || payload.length != 1) {
            //TODO: debug log
            return resultNoContent();
        }

        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
        var interactiveMessage = Json.fromJson(Json.parse(payload[0]), InteractiveMessage.class);

        if (interactiveMessage == null) {
            //TODO: debug log
            return resultNoContent();
        }

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), interactiveMessage.token)) {
            return resultBadRequest(messages, "error.request.not.verified");
        }

        if (interactiveMessage.callbackId == null || interactiveMessage.triggerId == null || interactiveMessage.actions.size() != 1) {
            //TODO: debug log
            return resultNoContent();
        }

        return handleUserAction(messages, interactiveMessage);
    }

    private CompletionStage<Result> handleUserAction(MessageHandler messages, InteractiveMessage iMessage) {

        if (isUserActionMissingValues(iMessage)) {
            // TODO: debug log
            return resultNoContent();
        }

        var action = iMessage.actions.get(0);

        var dbKey = new AnalyticsKey();
        dbKey.teamId = iMessage.team.id;
        dbKey.channelId = iMessage.channel.id;

        if (action.value.equals("no")) {
            _analyticsDb.incrementIgnoredMessageCounts(dbKey);

            if (iMessage.responseUrl == null) {
                // TODO: debug log
                return resultNoContent();
            }
            return _slackService.deleteMessage(iMessage).thenApplyAsync(slackResponse ->
                            slackResponse.ok ? ok(Json.toJson(slackResponse)) : badRequest(Json.toJson(slackResponse))
                    , _ec.current());
        }

        if (action.value.equals("learn_more")) {
            _analyticsDb.incrementLearnMoreMessageCounts(dbKey);

            return _slackService.postLearnMore(messages, iMessage).thenComposeAsync(response -> {
                var json = Json.toJson(response);
                return CompletableFuture.completedFuture( response.ok ? ok(json) : badRequest(json));
            }, _ec.current());

        }

        if (action.value.equals("yes")) {
            _analyticsDb.incrementCorrectedMessageCounts(dbKey);

            return handleReplaceMessage(messages, iMessage);
        }

        return resultNoContent();
    }

    public CompletionStage<Result> handleReplaceMessage(MessageHandler messages, InteractiveMessage iMessage) {

        // if we're directly replacing the message, we don't need to keep the original version
        // as the 'name' attribute of the action, and we can put the correction there instead.
        // then we can remove this second call to the bias correct service
        var originalPost = iMessage.actions.get(0).name;

        var correctorResult = _biasCorrector.getCorrection(originalPost);
        return correctorResult.thenComposeAsync(correction -> {

            if (correction.isEmpty()) {
                return resultNoContent();
            }

            var tokenKey = new TokenKey();
            tokenKey.teamId = iMessage.team.id;
            tokenKey.userId = iMessage.user.id;

            // todo: log if slackresponse.ok == false
            return _slackService.postReplacement(messages, iMessage, correction, _tokenDb.getUserToken(tokenKey))
                    .thenApplyAsync(slackResponse -> noContent(), _ec.current());
        }, _ec.current());
    }

    private static boolean isUserActionMissingValues(InteractiveMessage iMessage) {

        return iMessage.actions == null || iMessage.actions.get(0) == null || iMessage.actions.get(0).name == null
                || iMessage.team == null || iMessage.team.id == null
                || iMessage.channel == null || iMessage.channel.id == null
                || iMessage.user == null || iMessage.user.id == null || iMessage.user.name == null;

    }
}