import com.google.inject.AbstractModule;
import db.DbManager;
import db.RedisDbManager;
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
        bind(DbManager.class).to(RedisDbManager.class).asEagerSingleton();
    }
}
