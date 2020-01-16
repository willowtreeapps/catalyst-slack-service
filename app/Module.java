import com.google.inject.AbstractModule;
import db.AnalyticsHandler;
import db.TokenHandler;
import db.RedisDbHandler;
import services.AppService;
import services.BiasCorrector;
import services.MessageCorrector;
import services.SlackService;
import util.AppConfig;
import util.SlackConfig;

public class Module extends AbstractModule {

    protected void configure() {
        // Application
        bind(MessageCorrector.class).to(BiasCorrector.class);
        bind(AppService.class).to(SlackService.class);
        bind(AppConfig.class).to(SlackConfig.class).asEagerSingleton();
        bind(TokenHandler.class).to(RedisDbHandler.class).asEagerSingleton();
        bind(AnalyticsHandler.class).to(RedisDbHandler.class).asEagerSingleton();
    }
}
