package org.catalyst.slackservice.db;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.inject.Inject;
import java.util.Arrays;

public class RedisDbHandler implements TokenHandler {
    final Logger logger = LoggerFactory.getLogger(RedisDbHandler.class);

    private JedisPool _jedisPool;
    private static final String USER_TOKENS = "user_tokens";
    private static final String BOT_TOKENS = "bot_tokens";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    public RedisDbHandler(JedisPool jedisPool) {
        _jedisPool = jedisPool;
    }

    @Override
    public void setUserToken(TokenKey key, String token) {
        if (key == null || key.teamId == null || key.userId == null || token == null) {
            logger.error("set user token failed. teamId: {}, userId: {}, token null ? {}", key.teamId, key.userId, (token == null));
            return;
        }

        try (Jedis jedis = _jedisPool.getResource()) {
            jedis.hset( USER_TOKENS, format(key.teamId, key.userId), token);
        } catch(Exception e) {
            logger.error("error while setting user token for {} {}, {}", key.teamId, key.userId, e.getMessage());
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

        if (userToken == null) {
            logger.error("token for {} {} not found", key.teamId, key.userId);
        }

        return userToken;
    }

    @Override
    public void setBotInfo(String teamId, Bot bot) {
        if (teamId == null || bot.userId == null || bot.token == null || bot.teamName == null) {
            logger.error("set bot token failed. teamId: {}, botName: {}, token null ? {}", teamId, bot.userId, (bot.token == null));
            return;
        }

        try (Jedis jedis = _jedisPool.getResource()) {
            jedis.hset(BOT_TOKENS, teamId, OBJECT_MAPPER.writeValueAsString(bot));
        } catch (JsonProcessingException e) {
            logger.error("unable to set bot info for team {} {}", teamId, e.getMessage());
        }
    }

    @Override
    public Bot getBotInfo(String teamId) {
        if (teamId == null) {
            return null;
        }

        Bot bot = null;
        try (Jedis jedis = _jedisPool.getResource()) {
            var value = jedis.hget( BOT_TOKENS, teamId );
            if (value != null) {
                bot = OBJECT_MAPPER.readValue(value, Bot.class);
            }
        } catch (JsonProcessingException e) {
            logger.error("unable to process bot info for team {} {}", teamId, e.getMessage());
        }

        if (bot == null) {
            logger.error("bot info for {} not found", teamId);
        }

        return bot;
    }

    @Override
    public void deleteTokens(String teamId, String[] tokens) {
        if (teamId == null || tokens == null || tokens.length == 0) {
            logger.error("unable to delete tokens for team {}. token null? {}", teamId, tokens == null);
            return;
        }

        String[] newTokens = Arrays.stream(tokens).map(token -> format(teamId, token)).toArray(String[]::new);

        try (Jedis jedis = _jedisPool.getResource()) {
            logger.debug("deleting tokens {}", Arrays.asList(newTokens));
            jedis.hdel( USER_TOKENS, newTokens);
        } catch (Exception e) {
            logger.error("error deleting tokens {}", e.getMessage());
        }
    }

    private static String format(String v1, String v2) {
        return String.format("%s_%s", v1, v2);
    }
}
