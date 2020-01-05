import com.google.inject.AbstractModule;
import services.BiasCorrector;
import services.MessageCorrector;
import util.AppConfig;
import util.SlackConfig;

public class Module extends AbstractModule {
    protected void configure() {
        // Application
        bind(MessageCorrector.class).to(BiasCorrector.class);
        bind(AppConfig.class).to(SlackConfig.class).asEagerSingleton();
    }
}
