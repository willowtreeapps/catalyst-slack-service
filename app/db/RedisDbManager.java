package db;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import util.AppConfig;

import javax.inject.Inject;

public class RedisDbManager implements DbManager {

    private JedisPool _jedisPool;

    private static DateTimeFormatter DATE_FORMATTTER = DateTimeFormat.forPattern("yyyy-MM-dd");

    private static String today() {
        return DATE_FORMATTTER.print(System.currentTimeMillis());
    }

    @Inject
    public RedisDbManager(AppConfig config) {
        String redisHost = config.getRedisHost();
        int redisPort = config.getRedisPort();
        _jedisPool = new JedisPool(
                new JedisPoolConfig(),
                redisHost,
                redisPort
        );
    }

    @Override
    public void addUserToken(String teamId, String userId, String token) {
        try (Jedis jedis = _jedisPool.getResource()) {
            jedis.hset(
                    "user_tokens",
                    String.format("%s_%s", teamId, userId),
                    token
            );
        }
    }

    public String getUserToken(String teamId, String userId) {
        String userToken = null;
        try (Jedis jedis = _jedisPool.getResource()) {
            userToken = jedis.hget(
                    "user_tokens",
                    String.format("%s_%s", teamId, userId)
            );
        }
        return userToken;
    }

    public void updateMessageCounts(String teamId, String channelId) {
        updateCounts(teamId, channelId, "total");
    }

    private void updateCounts(String teamId, String channelId, String prefix) {
        String dayKey = today();
        try (Jedis jedis = _jedisPool.getResource()) {
            jedis.hincrBy(
                    String.format("%s_team_messages", prefix),
                    teamId,
                    1L
            );
            jedis.hincrBy(
                    String.format("%s_channel_messages", prefix),
                    String.format("%s_%s", teamId, channelId),
                    1L
            );
            jedis.hincrBy(
                    String.format("%s_team_daily_messages", prefix),
                    String.format("%s_%s", teamId, dayKey),
                    1L
            );
            jedis.hincrBy(
                    String.format("%s_channel_daily_messages", prefix),
                    String.format("%s_%s_%s", teamId, channelId, dayKey),
                    1L
            );
            jedis.hincrBy(
                    String.format("%s_daily_messages", prefix),
                    String.format("%s_%s", teamId, dayKey),
                    1L
            );
            jedis.incrBy(
                    String.format("%s_messages", prefix),
                    1L
            );
        }
    }
}
