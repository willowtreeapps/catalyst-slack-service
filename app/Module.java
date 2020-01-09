import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
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
        bind(String.class).annotatedWith(Names.named("BIAS_CORRECT_URL")).toInstance(System.getenv("BIAS_CORRECT_URL"));
    }
}
