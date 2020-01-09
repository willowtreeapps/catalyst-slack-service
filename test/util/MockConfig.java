package util;

public class MockConfig implements AppConfig {
    public String getToken() { return "valid_token_123"; }
    public String getBotId() { return "valid_bot_id"; }
    public String getBotUserName() { return "valid_bot_username"; }
    public String getPostUrl() { return "http://valid_post_url"; }
    public String getAppOauthToken() {return "valid_app_oauth_token"; }
    public String getBotOauthToken() {return "valid_bot_oauth_token"; }

}
