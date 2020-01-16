package db;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockDbHandler implements TokenHandler, AnalyticsHandler {
    private Map<String, String> _channelMessages = new ConcurrentHashMap<>();
    private Map<String, String> _userTokens = new ConcurrentHashMap<>();

    @Override
    public void incrementMessageCounts(AnalyticsKey key) {
        var formattedKey = String.format("%s_%s", key.teamId, key.channelId);
        var value = _channelMessages.get(formattedKey);
        var intValue = value == null ? 1 : Integer.valueOf(value).intValue() + 1;
        _channelMessages.put(formattedKey, String.valueOf(intValue));
    }

    @Override
    public void incrementIgnoredMessageCounts(AnalyticsKey key) {

    }

    @Override
    public void incrementLearnMoreMessageCounts(AnalyticsKey key) {

    }

    @Override
    public void incrementCorrectedMessageCounts(AnalyticsKey key) {

    }

    @Override
    public void setUserToken(TokenKey key, String token) {
        var formattedKey = String.format("%s_%s", key.teamId, key.userId);
        _userTokens.put(formattedKey, token);
    }

    @Override
    public String getUserToken(TokenKey key) {
        return _userTokens.get(String.format("%s_%s", key.teamId, key.userId));
    }
}
