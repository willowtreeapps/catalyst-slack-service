package util;

import com.typesafe.config.Config;

import javax.inject.Inject;

public class SlackConfig implements AppConfig {

    private Config _config;

    @Inject
    public SlackConfig(Config config) {
        _config = config;
    }

    public String getBiasCorrectUrl() {return _config.getString("bias_correct_url"); }
    public String getToken() {return _config.getString("slack_token");}
    public String getBotId() {return _config.getString("bot_id"); }
    public String getBotUserName() {return _config.getString("bot_username"); }
    public String getPostUrl() {return _config.getString("post_url"); }
    public String getAppOauthToken() {return _config.getString("app_oauth_token"); }
    public String getBotOauthToken() {return _config.getString("bot_oauth_token"); }
    public String getSigningSecret() {return _config.getString("signing_secret"); }
}
