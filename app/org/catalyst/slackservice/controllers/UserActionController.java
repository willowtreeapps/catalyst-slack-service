package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.db.AnalyticsHandler;
import org.catalyst.slackservice.db.AnalyticsKey;
import org.catalyst.slackservice.db.TokenHandler;
import org.catalyst.slackservice.db.TokenKey;
import org.catalyst.slackservice.domain.Action;
import org.catalyst.slackservice.domain.InteractiveMessage;
import org.catalyst.slackservice.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.services.MessageCorrector;

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

        var payload = body == null ? null : PayloadHelper.getFirstArrayValue(body.get(PAYLOAD));
        if (payload == null || payload.isEmpty()) {
            logger.debug("empty user action payload");
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

        if (interactiveMessage.callbackId == null || interactiveMessage.actions == null || interactiveMessage.actions.isEmpty()) {
            logger.error("null interactive message field");
            logger.debug(String.format("interactive message fields --> callbackId: %s, actions: %s",
                    interactiveMessage.callbackId, interactiveMessage.actions));
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_INTERACTIVE_MESSAGE_FIELDS);
        }

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), interactiveMessage.token)) {
            return ResultHelper.badRequest(messages, MessageHandler.REQUEST_NOT_VERIFIED);
        }

        return handleUserAction(messages, interactiveMessage);
    }

    private CompletionStage<Result> handleUserAction(MessageHandler messages, InteractiveMessage iMessage) {

        if (isUserActionMissingValues(iMessage)) {
            logger.error("null user action/team/channel/responseUrl value");
            logger.debug(String.format("user action values --> channel: %s, team: %s, user: %s, responseUrl: %s",
                iMessage.channel, iMessage.team, iMessage.user, iMessage.responseUrl));
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_USER_ACTION_VALUES);
        }

        var action = iMessage.actions.stream().findFirst().get();

        var dbKey = new AnalyticsKey();
        dbKey.teamId = iMessage.team.id;
        dbKey.channelId = iMessage.channel.id;

        if (action.value.equals(Action.NO)) {
            _analyticsDb.incrementIgnoredMessageCounts(dbKey);

            return _slackService.deleteMessage(iMessage.responseUrl).thenApplyAsync(slackResponse -> noContent(), _ec.current());
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

            var replacementResult = _slackService.postReplacement(messages, iMessage, correction, _tokenDb.getUserToken(tokenKey));

            return replacementResult.thenComposeAsync(replacementResponse -> {
                if (!replacementResponse.ok) {
                    logger.error("unable to replace message: " + Json.toJson(replacementResponse));
                    return ResultHelper.noContent();
                }

                logger.debug(String.format("message replaced: %s --> %s", originalPost, correction));
                return _slackService.deleteMessage(iMessage.responseUrl).thenApplyAsync(deleteResponse -> noContent(), _ec.current());
            }, _ec.current());
        }, _ec.current());
    }

    private static boolean isUserActionMissingValues(InteractiveMessage iMessage) {
        var actions = iMessage.actions.stream().findFirst();

        return actions.isEmpty() || actions.get().name == null
                || iMessage.team == null || iMessage.team.id == null
                || iMessage.channel == null || iMessage.channel.id == null
                || iMessage.user == null || iMessage.user.id == null || iMessage.user.name == null
                || iMessage.responseUrl == null;

    }
}