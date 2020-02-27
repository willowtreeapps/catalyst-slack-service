package org.catalyst.slackservice.util;

public interface AppConfig {
    String getToken();
    String getBiasCorrectUrl();
    String getPostMessageUrl();
    String getPostEphemeralUrl();
    String getSigningSecret();
    String getLearnMoreUrl();
    String getClientId();
    String getClientSecret();
    String getAppSigninUrl();
    String getRedisHost();
    int getRedisPort();
    String getOauthUrl();
    String getUpdateUrl();
    String getAuthorizedUrl();
    String getConversationsInfoUrl();
    String getTrackingId();
}
