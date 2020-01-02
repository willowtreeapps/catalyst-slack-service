package util;

import com.typesafe.config.Config;

import javax.inject.Inject;

public class SlackConfig implements AppConfig {

    private Config _config;

    @Inject
    public SlackConfig(Config config) {
        _config = config;
    }

    public String getToken() {
        return _config.getString("slack_token");
    }
}
