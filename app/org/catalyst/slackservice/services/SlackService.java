package org.catalyst.slackservice.services;

import org.catalyst.slackservice.domain.*;
import org.catalyst.slackservice.util.SlackLocale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSBodyReadables;
import play.libs.ws.WSClient;
import org.catalyst.slackservice.util.AppConfig;
import org.catalyst.slackservice.util.MessageGenerator;
import org.catalyst.slackservice.util.MessageHandler;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class SlackService implements AppService, WSBodyReadables {
    final Logger logger = LoggerFactory.getLogger(SlackService.class);

    private final static String CONTENT_TYPE_JSON = "application/json";
    private final static String HEADER_AUTHORIZATION = "Authorization";
    private final static String QUERY_PARAM_CODE = "code";
    private final static String QUERY_PARAM_ID = "client_id";
    private final static String QUERY_PARAM_SECRET = "client_secret";
    private final static String POST_DELETE_ORIGINAL = "delete_original";
    private final static String QUERY_PARAM_TOKEN = "token";
    private final static String QUERY_PARAM_CHANNEL = "channel";
    private final static String QUERY_PARAM_INCLUDE_LOCALE = "include_locale";


    private final WSClient _wsClient;
    private final AppConfig _config;
    private final HttpExecutionContext _ec;

    @Inject
    SlackService(HttpExecutionContext ec, AppConfig config, WSClient wsClient) {
        this._ec = ec;
        this._wsClient = wsClient;
        this._config = config;
    }

    private CompletionStage<SlackResponse> postReply(String url, Message reply, String authToken) {

        var request = _wsClient.url(url).setContentType(CONTENT_TYPE_JSON).
                addHeader(HEADER_AUTHORIZATION, String.format("Bearer %s", authToken));

        var jsonReply = Json.toJson(reply);
        var jsonPromise = request.post(jsonReply);

        return jsonPromise.thenApplyAsync(r -> {
            var response = r.getBody(json());
            logger.debug("\nposting to slack {} --> {}\nresponse --> {}", url, jsonReply, response);
            return Json.fromJson(response, SlackResponse.class);
        }, _ec.current());
    }

    @Override
    public CompletionStage<SlackResponse> postSuggestion(final MessageHandler messages, final Event event, final String correction) {
        var botReply = MessageGenerator.generateSuggestion(messages, event, correction, _config.getBotOauthToken());
        return postReply(_config.getPostEphemeralUrl(), botReply, _config.getBotOauthToken());
    }

    @Override
    public CompletionStage<SlackResponse> postChannelJoin(final MessageHandler messages, final Event event) {

        String url = _config.getPostEphemeralUrl();
        Message message = MessageGenerator.generateUserJoinedMessage(messages, event,
                _config.getBotOauthToken(), _config.getAppSigninUrl(), _config.getLearnMoreUrl());

        if (_config.getBotId().equals(event.user)) {
            url = _config.getPostMessageUrl();
            message = MessageGenerator.generatePluginAddedMessage(messages, event,
                _config.getBotOauthToken(), _config.getAppSigninUrl(), _config.getLearnMoreUrl());
        }

        return postReply(url, message, _config.getBotOauthToken());
    }

    @Override
    public CompletionStage<AuthResponse> getAuthorization(String requestCode) {
        var request = _wsClient.url(_config.getOauthUrl()).
                addQueryParameter(QUERY_PARAM_CODE, requestCode).
                addQueryParameter(QUERY_PARAM_ID, _config.getClientId()).
                addQueryParameter(QUERY_PARAM_SECRET, _config.getClientSecret());

        var jsonPromise = request.get();

        return jsonPromise.thenApplyAsync(r -> Json.fromJson(r.getBody(json()), AuthResponse.class), _ec.current());
    }

    @Override
    public CompletionStage<SlackResponse> postLearnMore(MessageHandler msg, InteractiveMessage iMessage) {
        var message = new Message();
        message.token = _config.getBotOauthToken();
        message.channel = iMessage.channel.id;
        message.text = msg.get(MessageHandler.LEARN_MORE);
        message.user = iMessage.user.id;

        return postReply(_config.getPostEphemeralUrl(), message, _config.getBotOauthToken());
    }

    @Override
    public CompletionStage<SlackResponse> postReplacement(InteractiveMessage iMessage, String userToken) {

        var message = new Message();
        message.token = userToken;
        message.channel = iMessage.channel.id;
        message.text = iMessage.actions.stream().findFirst().get().name; // corrected message
        message.ts = iMessage.callbackId;

        var url = _config.getUpdateUrl();

        return postReply(url, message, message.token);
    }

    @Override
    public CompletionStage<SlackResponse> deleteMessage(String responseUrl) {

        var request = _wsClient.url(responseUrl).setContentType(CONTENT_TYPE_JSON);
        var jsonPromise = request.post(Json.toJson(Map.of(POST_DELETE_ORIGINAL, Boolean.valueOf(true))));

        return jsonPromise.thenApplyAsync(r -> Json.fromJson(r.getBody(json()), SlackResponse.class), _ec.current());
    }

    @Override
    public CompletionStage<SlackResponse> postHelpMessage(MessageHandler messages, Event event) {
        var message = new Message();
        message.token = _config.getBotOauthToken();
        message.channel = event.channel;
        message.text = messages.get(MessageHandler.PLUGIN_INFO);

        return postReply(_config.getPostMessageUrl(), message, _config.getBotOauthToken());
    }

    @Override
    public CompletionStage<SlackLocale> getConversationLocale(String channel) {
        var request = _wsClient.url(_config.getConversationsInfoUrl()).
                addQueryParameter(QUERY_PARAM_TOKEN, _config.getBotOauthToken()).
                addQueryParameter(QUERY_PARAM_CHANNEL, channel).
                addQueryParameter(QUERY_PARAM_INCLUDE_LOCALE, "true");
        var jsonPromise = request.get();
        return jsonPromise.thenApplyAsync(r -> {
            if (r.getStatus() != 200 ) {
                logger.error("failed to retrieve user locale. {}", r.getStatus());
                return new SlackLocale();
            }

            var responseJson = r.getBody(json());
            var response = Json.fromJson(responseJson, ConversationsResponse.class);

            if (!response.ok || response.channel == null) {
                logger.error("failed to retrieve user locale. {}", responseJson);
                return new SlackLocale();
            }

            var localeCode = response.channel.locale;
            logger.debug("user locale " + localeCode);
            return new SlackLocale(localeCode);
        } , _ec.current());
    }
}
