package db;

public interface TokenHandler {
    void setUserToken(TokenKey key, String token);
    String getUserToken(TokenKey key);
}