package org.catalyst.slackservice.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.catalyst.slackservice.services.AnalyticsKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockDbHandler implements TokenHandler, AnalyticsHandler {
    private Map<String, String> _channelMessages = new ConcurrentHashMap<>();
    private Map<String, String> _userTokens = new ConcurrentHashMap<>();
    private Map<String, String> _botTokens = new ConcurrentHashMap<>();

    @Override
    public void setUserToken(TokenKey key, String token) {
        var formattedKey = String.format("%s_%s", key.teamId, key.userId);
        _userTokens.put(formattedKey, token);
    }

    @Override
    public String getUserToken(TokenKey key) {
        return _userTokens.get(String.format("%s_%s", key.teamId, key.userId));
    }

    @Override
    public void setTeamName(String teamId, String teamName) {
    }

    @Override
    public void setBotInfo(String teamId, Bot bot) {
        try {
            _botTokens.put(teamId, new ObjectMapper().writeValueAsString(bot));
        } catch (JsonProcessingException e) {}
    }

    @Override
    public Bot getBotInfo(String teamId) {
        if (teamId == null) {
            return null;
        }

        Bot bot = null;
        try {
            var value = _botTokens.get(teamId);
            if (value != null) {
                bot = new ObjectMapper().readValue(value, Bot.class);
            }
        } catch (JsonProcessingException e) {}
        return bot;
    }

    @Override
    public void deleteTokens(String teamId, String[] tokens) {
        if (teamId == null || tokens == null || tokens.length == 0) {
            return;
        }

        for (String token: tokens) {
            _userTokens.remove(String.format("%s_%s", teamId, token));
        }
    }
}
