package db;

import com.google.inject.Inject;
import com.google.inject.Provider;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import util.AppConfig;

public class JedisPoolProvider implements Provider<JedisPool> {

    JedisPool pool;

    @Inject
    public JedisPoolProvider(AppConfig config) {
        pool = new JedisPool(new JedisPoolConfig(), config.getRedisHost(), config.getRedisPort());
    }

    @Override
    public JedisPool get() {
        return pool;
    }
}
