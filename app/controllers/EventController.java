package controllers;

import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AppService;
import services.MessageCorrector;
import util.AppConfig;
import util.MessageHandler;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;

public class EventController extends Controller {

    public static class Request {
        public String token;
        public String challenge;
        public String type;
        public Event event;
    }

    //TODO: get from within event.blocks[{elements:[{}]]}]
    public static class Event {
        public String client_msg_id;
        public String type;
        public String subtype;
        public String text;
        public String user;
        public String ts;
        public String team;
        public String username;
        public String bot_id;
        public String channel;
        public String event_ts;
        public String channel_type;
    }

    private final AppConfig _config;
    private final MessagesApi _messagesApi;
    private final MessageCorrector _biasCorrector;
    private final AppService _slackService;

    @Inject
    public EventController(AppConfig config, MessagesApi messagesApi, MessageCorrector biasCorrector, AppService slackService) {
        this._config = config;
        this._messagesApi = messagesApi;
        this._biasCorrector = biasCorrector;
        this._slackService = slackService;
    }

    public Result handle(Http.Request httpRequest) {
        var messages = new MessageHandler(_messagesApi.preferred(httpRequest));
        var optionalRequest = httpRequest.body().parseJson(Request.class);
        var error = validateRequest(optionalRequest);

        if (error != null) {
            return badRequest(messages.error(error));
        }

        var eventRequest = optionalRequest.get();
        if (eventRequest.type.equals("url_verification")) {
            return handleURLVerification(messages, eventRequest.challenge);
        } else if (eventRequest.type.equals("event_callback")) {
            return handleEventCallback(messages, eventRequest.event);
        }

        return badRequest(messages.error("error.unsupported.type"));
    }

    private String validateRequest(final Optional<Request> request) {
        if (request.isEmpty()) {
            return "error.invalid.request";
        }

        if (request.filter(r -> r.token == null || !r.token.equals(_config.getToken())).isPresent()) {
            return "error.invalid.token";
        }

        if (request.filter(r -> r.type == null).isPresent()) {
            return "error.missing.type";
        }

        return null;
    }

    /**
     * URL verification happens during configuration of the app Event Subscription URL
     * @param messages
     * @param challenge
     * @return
     */
    private Result handleURLVerification(final MessageHandler messages, final String challenge) {
        if (challenge == null) {
            return badRequest(messages.error("error.missing.challenge"));
        }

        return ok(Json.toJson(Map.of("challenge", challenge)));
    }

    /**
     * Event callback is triggered for all subscribed events
     * @param messages
     * @param event
     * @return
     */
    private Result handleEventCallback(final MessageHandler messages, final Event event) {
        if (event == null) {
            return badRequest(messages.error("error.invalid.event"));
        }

        var userName = event.username;
        var botId = event.bot_id;

        boolean isBotMessage = botId != null && botId.equals(_config.getBotId()) &&
            userName != null && userName.equals(_config.getBotUserName());

        if (isBotMessage || event.user == null || event.text == null) {
            return ok();
        }

        if (event.subtype == null) {
            //TODO: handle help request direct im to slackbot
            //TODO: handle channel_join
            handleUserMessage(messages, event);
        }

        return ok(messages.toJson("ok", "true"));
    }

    private void handleUserMessage(final MessageHandler messages, final Event event) {
        if (event.team != null && event.channel != null) {
            //TODO: update message counts
        }
        var correction = _biasCorrector.getCorrection(event.text);
        if (correction.isEmpty()) {
            return;
        }

        //TODO: check for team tokens as auth parameter? see original code for reference
        var botReply = _slackService.generateSuggestion(messages, event.ts, event.channel, event.user, _config.getAppOauthToken(), correction );
        _slackService.postReply(botReply, _config.getAppOauthToken());
    }
}
