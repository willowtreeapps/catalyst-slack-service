import com.google.inject.AbstractModule;
import org.catalyst.slackservice.db.AnalyticsHandler;
import org.catalyst.slackservice.db.TokenHandler;
import org.catalyst.slackservice.db.RedisDbHandler;
import redis.clients.jedis.JedisPool;
import org.catalyst.slackservice.services.AppService;
import org.catalyst.slackservice.services.BiasCorrector;
import org.catalyst.slackservice.services.MessageCorrector;
import org.catalyst.slackservice.services.SlackService;
import org.catalyst.slackservice.util.AppConfig;
import org.catalyst.slackservice.db.JedisPoolProvider;
import org.catalyst.slackservice.util.SlackConfig;

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
