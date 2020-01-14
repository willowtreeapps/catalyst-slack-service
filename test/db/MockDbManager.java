package db;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockDbManager implements DbManager {
    private Map<String, String> _channelMessages = new ConcurrentHashMap<>();
    private Map<String, String> _userTokens = new ConcurrentHashMap<>();

    @Override
    public void updateMessageCounts(String teamId, String channelId) {
        var key = String.format("%s_%s", teamId, channelId);
        var value = _channelMessages.get(key);
        var intValue = value == null ? 1 : Integer.valueOf(value).intValue() + 1;
        _channelMessages.put(key, String.valueOf(intValue));

    }

    @Override
    public void addUserToken(String teamId, String userId, String token) {
        var key = String.format("%s_%s", teamId, userId);
        _userTokens.put(key, token);
    }

    @Override
    public String getUserToken(String teamId, String userId) {
        return _userTokens.get(String.format("%s_%s", teamId, userId));
    }
}
