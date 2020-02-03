package org.catalyst.slackservice.controllers;

import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class HelpController extends Controller {
    final Logger logger = LoggerFactory.getLogger(HelpController.class);

    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final AppService _slackService;

    private final static String BIAS_CORRECT = "/bias-correct-v2";
    private final static String TOKEN = "token";
    private final static String COMMAND = "command";
    private final static String TEXT = "text";
    private final static String HELP = "help";
    private static final String CHANNEL_ID = "channel_id";

    @Inject
    public HelpController(AppConfig config, MessagesApi messagesApi, AppService service) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._slackService = service;
    }

    public CompletionStage<Result> handle(Http.Request httpRequest) {
        var body = httpRequest.body().asFormUrlEncoded();

        if (body == null) {
            return ResultHelper.noContent();
        }

        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
        var token = PayloadHelper.getFirstArrayValue(body.get(TOKEN));
        if (token.isEmpty()) {
            logger.error("empty token");
            return ResultHelper.badRequest(messages, MessageHandler.REQUEST_NOT_VERIFIED);
        }

        if (!RequestVerifier.verified(httpRequest, _config.getSigningSecret(), _config.getToken(), token.get())) {
            logger.error("request not verified");
            return ResultHelper.badRequest(messages, MessageHandler.REQUEST_NOT_VERIFIED);
        }

        var command = PayloadHelper.getFirstArrayValue(body.get(COMMAND));
        var text = PayloadHelper.getFirstArrayValue(body.get(TEXT));
        var channel = PayloadHelper.getFirstArrayValue(body.get(CHANNEL_ID));

        logger.debug("command: {}, text: {}, channel: {}", command, text, channel);

        if (command.isEmpty() || !command.get().equals(BIAS_CORRECT) || channel.isEmpty()) {
            return ResultHelper.noContent();
        }

        return handleHelpRequest(channel.get(), text);
    }

    private CompletionStage<Result> handleHelpRequest(String channel, Optional<String> text) {
        var localeResult = _slackService.getConversationLocale(channel);

        return localeResult.thenComposeAsync(slackLocale -> {
            var localizedMessages = new MessageHandler(_messagesApi, slackLocale);
            var message = localizedMessages.get(MessageHandler.PLUGIN_INFO);

            if (text.isEmpty() || text.get().isEmpty()) {
                message = localizedMessages.get(MessageHandler.SPECIFY_ACTION);
            } else if (!text.get().equals(HELP)) {
                message = localizedMessages.get(MessageHandler.UNSUPPORTED_ACTION, text.get());
            }

            return ResultHelper.ok(Json.toJson(Map.of(TEXT, message)));
        });
    }
}