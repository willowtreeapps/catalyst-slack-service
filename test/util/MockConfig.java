package util;

public class MockConfig implements AppConfig {

    @Override
    public String getToken() {
        return "valid_token_123";
    }
}
