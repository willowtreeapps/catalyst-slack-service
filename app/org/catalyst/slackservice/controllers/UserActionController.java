package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.db.*;
import org.catalyst.slackservice.domain.Action;
import org.catalyst.slackservice.domain.InteractiveMessage;
import org.catalyst.slackservice.services.AnalyticsEvent;
import org.catalyst.slackservice.services.AnalyticsKey;
import org.catalyst.slackservice.services.AnalyticsService;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.concurrent.CompletionStage;

public class UserActionController extends Controller {
    final Logger logger = LoggerFactory.getLogger(UserActionController.class);

    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final AppService _slackService;
    private final HttpExecutionContext _ec;
    private final TokenHandler _tokenDb;
    private final AnalyticsService _analyticsService;

    private final static String PAYLOAD = "payload";

    @Inject
    public UserActionController(HttpExecutionContext ec, AppConfig config, MessagesApi messagesApi,
                                AppService slackService, TokenHandler tokenDb, AnalyticsService analyticsService) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._slackService = slackService;
        this._ec = ec;
        this._tokenDb = tokenDb;
        this._analyticsService = analyticsService;
    }

    @BodyParser.Of(BodyParser.Raw.class)
    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
        var requestBodyAsBytes = httpRequest.body().asBytes();
        if (requestBodyAsBytes == null || requestBodyAsBytes.isEmpty()) {
            logger.error("empty user action content");
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_REQUEST);
        }

        var body = PayloadHelper.getFormUrlEncodedRequestBody(requestBodyAsBytes.decodeString(PayloadHelper.CHARSET_UTF8));

        var payload = PayloadHelper.getMapValue(body, PAYLOAD);
        if (payload.isEmpty()) {
            logger.error("empty user action payload");
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_REQUEST);
        }

        // TODO: move outside of controller
        InteractiveMessage interactiveMessage = null;
        try {
            interactiveMessage = Json.fromJson(Json.parse(payload), InteractiveMessage.class);
        } catch(Exception e) {
            logger.error(e.getMessage());
        }

        if (interactiveMessage == null) {
            logger.error("null interactive message");
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_REQUEST);
        }

        if (interactiveMessage.callbackId == null || interactiveMessage.actions == null || interactiveMessage.actions.isEmpty()) {
            logger.error("null interactive message field");
            logger.debug("interactive message fields --> callbackId: {}, actions: {}", interactiveMessage.callbackId, interactiveMessage.actions);
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_INTERACTIVE_MESSAGE_FIELDS);
        }

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), interactiveMessage.token)) {
            return ResultHelper.badRequest(messages, MessageHandler.REQUEST_NOT_VERIFIED);
        }

        if (isUserActionMissingValues(interactiveMessage)) {
            logger.error("null user action/team/channel/responseUrl value");
            logger.debug("user action values --> channel: {}, team: {}, user: {}, responseUrl: {}",
                    interactiveMessage.channel, interactiveMessage.team, interactiveMessage.user, interactiveMessage.responseUrl);
            return ResultHelper.badRequest(messages, MessageHandler.MISSING_USER_ACTION_VALUES);
        }

        return handleUserAction(interactiveMessage);
    }

    private CompletionStage<Result> handleUserAction(final InteractiveMessage iMessage) {
        var bot = _tokenDb.getBotInfo(iMessage.team.id);
        if (bot == null || bot.userId == null || bot.token == null) {
            return ResultHelper.ok();
        }

        var action = iMessage.actions.stream().findFirst().get();
        var localeResult = _slackService.getConversationLocale(iMessage.channel.id, bot);

        return localeResult.thenComposeAsync(slackLocale -> {
            var key = new AnalyticsKey(_config.getTrackingId(), iMessage.team.id, bot.teamName, iMessage.channel.id, iMessage.user.id, slackLocale);
            try {
                var event = AnalyticsEvent.createMessageActionEvent(key, action);
                _analyticsService.track(event);
            }
            catch (Exception e) {
                logger.error("failed to track user action analytics event {}", action);
            }

            var localizedMessages = new MessageHandler(_messagesApi, slackLocale);
            if (action.value.equals(Action.NO)) {
                return _slackService.deleteMessage(iMessage.responseUrl).thenApplyAsync(slackResponse -> noContent(), _ec.current());
            }

            if (action.value.equals(Action.LEARN_MORE)) {
                return _slackService.postLearnMore(localizedMessages, iMessage, bot).thenApplyAsync(response -> noContent(), _ec.current());
            }

            if (action.value.equals(Action.YES)) {
                return handleReplaceMessage(iMessage);
            }

            return ResultHelper.noContent();
        });
    }

    private CompletionStage<Result> handleReplaceMessage(final InteractiveMessage iMessage) {

        var correction = iMessage.actions.stream().findFirst().get().name;
        var tokenKey = new TokenKey();
        tokenKey.teamId = iMessage.team.id;
        tokenKey.userId = iMessage.user.id;

        var replacementResult = _slackService.postReplacement(iMessage, _tokenDb.getUserToken(tokenKey));

        return replacementResult.thenComposeAsync(replacementResponse -> {
            if (!replacementResponse.ok) {
                logger.error("unable to replace message for {} {}", tokenKey.teamId, tokenKey.userId);
                return ResultHelper.noContent();
            }

            logger.debug("message replaced: --> {}", correction);
            return _slackService.deleteMessage(iMessage.responseUrl).thenApplyAsync(deleteResponse -> noContent(), _ec.current());
        }, _ec.current());
    }

    private static boolean isUserActionMissingValues(final InteractiveMessage iMessage) {
        var actions = iMessage.actions.stream().findFirst();

        return actions.isEmpty() || actions.get().name == null
                || iMessage.team == null || iMessage.team.id == null
                || iMessage.channel == null || iMessage.channel.id == null
                || iMessage.user == null || iMessage.user.id == null || iMessage.user.name == null
                || iMessage.responseUrl == null;

    }
}