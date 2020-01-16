package db;

public interface DbManager {
    void updateMessageCounts(String teamId, String channelId);
    void addUserToken(String teamId, String userId, String token);
    String getUserToken(String teamId, String userId);
}