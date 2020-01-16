import com.google.inject.AbstractModule;
import db.AnalyticsHandler;
import db.TokenHandler;
import db.RedisDbHandler;
import redis.clients.jedis.JedisPool;
import services.AppService;
import services.BiasCorrector;
import services.MessageCorrector;
import services.SlackService;
import util.AppConfig;
import db.JedisPoolProvider;
import util.SlackConfig;

public class Module extends AbstractModule {

    protected void configure() {
        // Application
        bind(MessageCorrector.class).to(BiasCorrector.class);
        bind(AppService.class).to(SlackService.class);
        bind(AppConfig.class).to(SlackConfig.class).asEagerSingleton();
        bind(TokenHandler.class).to(RedisDbHandler.class).asEagerSingleton();
        bind(AnalyticsHandler.class).to(RedisDbHandler.class).asEagerSingleton();
        bind(JedisPool.class).toProvider(JedisPoolProvider.class).asEagerSingleton();
    }
}
