package util;

public class MockConfig implements AppConfig {
    public String getToken() { return "valid_token_123"; }
    public String getBiasCorrectUrl() {
        return "http://valid_url";
    }
    public String getBotId() {return "valid_bot_id"; }
    public String getBotUserName() {return "valid_bot_username"; }
}
