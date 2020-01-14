package db;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import util.AppConfig;

import javax.inject.Inject;

public class RedisDbManager implements DbManager {

    protected JedisPool _jedisPool;
    private static final DateTimeFormatter DATE_FORMATTTER = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final String USER_TOKENS = "user_tokens";

    @Inject
    public RedisDbManager(AppConfig config) {
        _jedisPool = new JedisPool( new JedisPoolConfig(), config.getRedisHost(), config.getRedisPort() );
    }

    @Override
    public void addUserToken(String teamId, String userId, String token) {
        if (teamId == null || userId == null || token == null) {
            //TODO: log
            return;
        }

        try (Jedis jedis = _jedisPool.getResource()) {
            jedis.hset( USER_TOKENS, format(teamId, userId), token);
        }
    }

    public String getUserToken(String teamId, String userId) {
        String userToken = null;
        try (Jedis jedis = _jedisPool.getResource()) {
            userToken = jedis.hget( USER_TOKENS, format(teamId, userId) );
        }
        return userToken;
    }

    public void updateMessageCounts(String teamId, String channelId) {
        if (teamId == null || channelId == null) {
            //TODO: log
            return;
        }

        updateCounts(teamId, channelId, "total");
    }

    private void updateCounts(String teamId, String channelId, String prefix) {
        var dayKey = today();
        try (Jedis jedis = _jedisPool.getResource()) {
            jedis.hincrBy( format(prefix, "team_messages"), teamId,1L );
            jedis.hincrBy( format(prefix, "channel_messages"), format(teamId, channelId),1L );
            jedis.hincrBy( format(prefix, "team_daily_messages"), format(teamId, dayKey), 1L );
            jedis.hincrBy( format(prefix, "channel_daily_messages"), String.format("%s_%s_%s", teamId, channelId, dayKey), 1L );
            jedis.hincrBy( format(prefix, "daily_messages"), format(teamId, dayKey), 1L );
            jedis.incrBy( format(prefix, "messages"), 1L );
        }
    }

    private static String today() {
        return DATE_FORMATTTER.print(System.currentTimeMillis());
    }

    private static String format(String v1, String v2) {
        return String.format("%s_%s", v1, v2);
    }
}
