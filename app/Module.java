import com.google.inject.AbstractModule;
import util.AppConfig;
import util.SlackConfig;

public class Module extends AbstractModule {
    protected void configure() {
        // Application
        bind(AppConfig.class).to(SlackConfig.class).asEagerSingleton();
    }
}
