package controllers;

import db.AnalyticsHandler;
import db.AnalyticsKey;
import db.TokenHandler;
import db.TokenKey;
import domain.Action;
import domain.InteractiveMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AppService;
import services.MessageCorrector;
import util.*;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class UserActionController extends Controller {
    final Logger logger = LoggerFactory.getLogger(UserActionController.class);

    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final MessageCorrector _biasCorrector;
    private final AppService _slackService;
    private final HttpExecutionContext _ec;
    private final TokenHandler _tokenDb;
    private final AnalyticsHandler _analyticsDb;

    private final static String PAYLOAD = "payload";

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

    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var body = httpRequest.body().asFormUrlEncoded();

        if (body == null) {
            return ResultHelper.noContent();
        }

        var payload = PayloadHelper.getFirstArrayValue(body.get(PAYLOAD));
        if (payload.isEmpty()) {
            logger.debug("empty payload");
            return ResultHelper.noContent();
        }

        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
        InteractiveMessage interactiveMessage = null;
        try {
            interactiveMessage = Json.fromJson(Json.parse(payload.get()), InteractiveMessage.class);
        } catch(Exception e) {
            logger.error(e.getMessage());
        }

        if (interactiveMessage == null) {
            logger.error("null interactive message");
            return ResultHelper.noContent();
        }

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), interactiveMessage.token)) {
            return ResultHelper.badRequest(messages, MessageHandler.REQUEST_NOT_VERIFIED);
        }

        if (interactiveMessage.callbackId == null || interactiveMessage.triggerId == null ||
                interactiveMessage.actions == null || interactiveMessage.actions.isEmpty()) {
            logger.error("null interactive message field");
            logger.debug(String.format("interactive message fields --> callbackId: %s, triggerId: %s, actions: %s",
                    interactiveMessage.callbackId, interactiveMessage.triggerId, interactiveMessage.actions));
            return ResultHelper.noContent();
        }

        return handleUserAction(messages, interactiveMessage);
    }

    private CompletionStage<Result> handleUserAction(MessageHandler messages, InteractiveMessage iMessage) {

        if (isUserActionMissingValues(iMessage)) {
            logger.error("null user action/team/channel value");
            logger.debug(String.format("user action values --> channel: %s, team: %s, user: %s", iMessage.channel, iMessage.team, iMessage.user));
            return ResultHelper.noContent();
        }

        var action = iMessage.actions.stream().findFirst().get();

        var dbKey = new AnalyticsKey();
        dbKey.teamId = iMessage.team.id;
        dbKey.channelId = iMessage.channel.id;

        if (action.value.equals(Action.NO)) {
            _analyticsDb.incrementIgnoredMessageCounts(dbKey);

            if (iMessage.responseUrl == null) {
                logger.error("null response url, unable to delete message");
                return ResultHelper.noContent();
            }
            return _slackService.deleteMessage(iMessage).thenApplyAsync(slackResponse ->
                            slackResponse.ok ? ok(Json.toJson(slackResponse)) : badRequest(Json.toJson(slackResponse))
                    , _ec.current());
        }

        if (action.value.equals(Action.LEARN_MORE)) {
            _analyticsDb.incrementLearnMoreMessageCounts(dbKey);

            return _slackService.postLearnMore(messages, iMessage).thenApplyAsync(response -> noContent(), _ec.current());

        }

        if (action.value.equals(Action.YES)) {
            _analyticsDb.incrementCorrectedMessageCounts(dbKey);

            return handleReplaceMessage(messages, iMessage);
        }

        return ResultHelper.noContent();
    }

    public CompletionStage<Result> handleReplaceMessage(MessageHandler messages, InteractiveMessage iMessage) {

        // if we're directly replacing the message, we don't need to keep the original version
        // as the 'name' attribute of the action, and we can put the correction there instead.
        // then we can remove this second call to the bias correct service
        var originalPost = iMessage.actions.stream().findFirst().get().name;

        var correctorResult = _biasCorrector.getCorrection(originalPost);
        return correctorResult.thenComposeAsync(correction -> {

            if (correction.isEmpty()) {
                return ResultHelper.noContent();
            }

            var tokenKey = new TokenKey();
            tokenKey.teamId = iMessage.team.id;
            tokenKey.userId = iMessage.user.id;

            return _slackService.postReplacement(messages, iMessage, correction, _tokenDb.getUserToken(tokenKey))
                    .thenApplyAsync(slackResponse -> {
                        if (!slackResponse.ok) {
                            logger.error("unable to replace message: " + Json.toJson(slackResponse));
                        }

                        logger.debug(String.format("message replaced: %s --> %s", originalPost, correction));
                        return noContent();
                    }, _ec.current());
        }, _ec.current());
    }

    private static boolean isUserActionMissingValues(InteractiveMessage iMessage) {
        var actions = iMessage.actions.stream().findFirst();

        return actions.isEmpty() || actions.get().name == null
                || iMessage.team == null || iMessage.team.id == null
                || iMessage.channel == null || iMessage.channel.id == null
                || iMessage.user == null || iMessage.user.id == null || iMessage.user.name == null;

    }
}