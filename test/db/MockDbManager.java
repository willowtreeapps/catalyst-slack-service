package db;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockDbManager implements DbManager {
    private Map<String, String> _channelMessages = new ConcurrentHashMap<>();

    @Override
    public void updateMessageCounts(String teamId, String channelId) {
        String key = String.format("%s_%s", teamId, channelId);
        String value = _channelMessages.get(key);
        int intValue = value == null ? 1 : Integer.valueOf(value).intValue() + 1;
        _channelMessages.put(key, String.valueOf(intValue));

    }

    @Override
    public void addUserToken(String teamId, String userId, String token) {

    }

    @Override
    public String getUserToken(String teamId, String userId) {
        return null;
    }
}
