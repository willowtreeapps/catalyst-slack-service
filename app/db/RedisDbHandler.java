package db;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class RedisDbHandler implements TokenHandler, AnalyticsHandler {
    final Logger logger = LoggerFactory.getLogger(RedisDbHandler.class);

    private JedisPool _jedisPool;
    private static final ZoneId ET = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String USER_TOKENS = "user_tokens";

    @Inject
    public RedisDbHandler(JedisPool jedisPool) {
        _jedisPool = jedisPool;
    }

    @Override
    public void setUserToken(TokenKey key, String token) {
        if (key == null || key.teamId == null || key.userId == null || token == null) {
            logger.error("set user token failed. teamId: "+ key.teamId + ", userId: " + key.userId + " token null ? " + (token == null));
            return;
        }

        try (Jedis jedis = _jedisPool.getResource()) {
            jedis.hset( USER_TOKENS, format(key.teamId, key.userId), token);
        }
    }

    @Override
    public String getUserToken(TokenKey key) {
        if (key == null || key.teamId == null || key.userId == null) {
            return null;
        }

        String userToken = null;
        try (Jedis jedis = _jedisPool.getResource()) {
            userToken = jedis.hget( USER_TOKENS, format(key.teamId, key.userId) );
        }

        logger.debug("token for " + key.teamId + " " + key.userId + ((userToken == null) ? " not found" : " found"));
        return userToken;
    }

    @Override
    public void incrementMessageCounts(AnalyticsKey key) {
        incrementCounts(key, "total");
    }

    @Override
    public void incrementIgnoredMessageCounts(AnalyticsKey key) {
        incrementCounts(key, "ignored");
    }

    @Override
    public void incrementLearnMoreMessageCounts(AnalyticsKey key) {
        incrementCounts(key, "learn_more");
    }

    @Override
    public void incrementCorrectedMessageCounts(AnalyticsKey key) {
        incrementCounts(key, "corrected");
    }

    private void incrementCounts(AnalyticsKey key, String prefix) {
        if (key == null || key.teamId == null || key.channelId == null) {
            logger.error("increment " + prefix + " counts failed. teamId: "+ key.teamId + ", channelId: " + key.channelId);
            return;
        }
        var dayKey = today();
        try (Jedis jedis = _jedisPool.getResource()) {
            jedis.hincrBy( format(prefix, "team_messages"), key.teamId,1L );
            jedis.hincrBy( format(prefix, "channel_messages"), format(key.teamId, key.channelId),1L );
            jedis.hincrBy( format(prefix, "team_daily_messages"), format(key.teamId, dayKey), 1L );
            jedis.hincrBy( format(prefix, "channel_daily_messages"), String.format("%s_%s_%s", key.teamId, key.channelId, dayKey), 1L );
            jedis.hincrBy( format(prefix, "daily_messages"), format(key.teamId, dayKey), 1L );
            jedis.incrBy( format(prefix, "messages"), 1L );
        }
    }

    private static String today() {
        LocalDate localDate = LocalDate.now(ET);
        return localDate.format(DATE_FORMATTER);
    }

    private static String format(String v1, String v2) {
        return String.format("%s_%s", v1, v2);
    }
}
