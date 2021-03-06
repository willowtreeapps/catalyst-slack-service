package org.catalyst.slackservice.util;

public class MockConfig implements AppConfig {
    public String getToken() { return "valid_token_123"; }
    // do not include http:// to avoid dns searching by unit test
    public String getBiasCorrectUrl() { return "/corrector/correct"; }
    public String getPostMessageUrl() { return "/api/chat.postMessage"; }
    public String getPostEphemeralUrl() { return "/api/chat.postEphemeral"; }
    public String getSigningSecret() {return "signing_secret"; }
    public String getLearnMoreUrl() {return "/learn_more"; }
    public String getClientId() {return "valid_client_id"; }
    public String getClientSecret() {return "valid_client_secret"; }
    public String getAppSigninUrl() {return"/oauth/authorize";}
    public String getRedisHost() {return ""; }
    public int getRedisPort() { return 0; }
    public String getOauthUrl() {return "/api/oauth.access";}
    public String getUpdateUrl() {return "/api/chat.update";}
    public String getAuthorizedUrl() { return "/authorized"; }
    public String getConversationsInfoUrl() {return "/api/conversations.info";}
    public String getTrackingId() {return "UA-XXXX-Y"; }
}
