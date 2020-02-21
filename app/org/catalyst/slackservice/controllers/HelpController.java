package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.db.TokenHandler;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class HelpController extends Controller {
    final Logger logger = LoggerFactory.getLogger(HelpController.class);

    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final AppService _slackService;
    private final TokenHandler _tokenDb;

    private final static String BIAS_CORRECT = "/bias-correct";
    private final static String TOKEN = "token";
    private final static String COMMAND = "command";
    private final static String TEXT = "text";
    private final static String HELP = "help";
    private static final String CHANNEL_ID = "channel_id";
    private static final String TEAM_ID = "team_id";

    @Inject
    public HelpController(AppConfig config, MessagesApi messagesApi, AppService service, TokenHandler tokenDb) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._slackService = service;
        this._tokenDb = tokenDb;
    }

    @BodyParser.Of(BodyParser.Raw.class)
    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));

        var requestBodyAsBytes = httpRequest.body().asBytes();
        if (requestBodyAsBytes == null || requestBodyAsBytes.isEmpty()) {
            logger.error("empty help request content");
            return ResultHelper.badRequest(messages, MessageHandler.INVALID_REQUEST);
        }

        var body = PayloadHelper.getFormUrlEncodedRequestBody(requestBodyAsBytes.decodeString(PayloadHelper.CHARSET_UTF8));
        var token = PayloadHelper.getMapValue(body, TOKEN);

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), token)) {
            logger.error("request not verified");
            return ResultHelper.badRequest(messages, MessageHandler.REQUEST_NOT_VERIFIED);
        }

        var command = PayloadHelper.getMapValue(body, COMMAND);
        var text = PayloadHelper.getMapValue(body, TEXT);
        var channel = PayloadHelper.getMapValue(body, CHANNEL_ID);
        var team = PayloadHelper.getMapValue(body, TEAM_ID);

        logger.debug("command: {}, text: {}, channel: {}", command, text, channel);

        if (command.isEmpty() || !command.equals(BIAS_CORRECT) || channel.isEmpty()) {
            return ResultHelper.noContent();
        }

        return handleHelpRequest(channel, text, team);
    }

    private CompletionStage<Result> handleHelpRequest(String channel, String text, String team) {
        var bot = _tokenDb.getBotInfo(team);
        if (bot == null || bot.userId == null || bot.token == null) {
            return ResultHelper.noContent();
        }

        var localeResult = _slackService.getConversationLocale(channel, bot);

        return localeResult.thenComposeAsync(slackLocale -> {
            var localizedMessages = new MessageHandler(_messagesApi, slackLocale);
            var message = localizedMessages.get(MessageHandler.PLUGIN_INFO);

            if (text.isEmpty()) {
                message = localizedMessages.get(MessageHandler.SPECIFY_ACTION);
            } else if (!text.equals(HELP)) {
                message = localizedMessages.get(MessageHandler.UNSUPPORTED_ACTION, text);
            }

            return ResultHelper.ok(Json.toJson(Map.of(TEXT, message)));
        });
    }
}