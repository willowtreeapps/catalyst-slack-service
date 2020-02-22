package org.catalyst.slackservice.db;

public interface TokenHandler {
    void setUserToken(TokenKey key, String token);
    String getUserToken(TokenKey key);

    void setBotInfo(String teamId, Bot bot);
    Bot getBotInfo(String teamId);
    void deleteTokens(String teamId, String[] tokens);
}