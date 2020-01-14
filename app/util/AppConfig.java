package util;

public interface AppConfig {
    String getToken();
    String getBiasCorrectUrl();
    String getBotId();
    String getBotUserName();
    String getPostMessageUrl();
    String getPostEphemeralUrl();
    String getAppOauthToken();
    String getBotOauthToken();
    String getSigningSecret();
    String getLearnMoreUrl();
    String getClientId();
    String getClientSecret();
    String getAppSigninUrl();
    String getRedisHost();
    int getRedisPort();
    String getOauthUrl();
}
