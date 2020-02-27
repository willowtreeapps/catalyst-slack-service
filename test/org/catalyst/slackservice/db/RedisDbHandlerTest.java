package org.catalyst.slackservice.db;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisDbHandlerTest {

    RedisDbHandler dbManager;
    Jedis jedisMock;

    @Before
    public void setup() {
        var jedisPool = Mockito.mock(JedisPool.class);
        jedisMock = Mockito.mock(Jedis.class);
        dbManager = new RedisDbHandler(jedisPool);
        Mockito.when(jedisPool.getResource()).thenReturn(jedisMock);
        Mockito.when(jedisMock.hget("user_tokens", "TEAM123_USER123")).thenReturn("xoxp-token-1234");
    }

    @Test
    public void testGetUserToken() {
        var key = new TokenKey();
        key.teamId = "TEAM123";
        key.userId = "USER123";
        var token = dbManager.getUserToken(key);
        Assert.assertEquals("xoxp-token-1234", token);
    }

    @Test
    public void testGetNullUserToken() {
        var key = new TokenKey();
        key.teamId = "TEAM123";
        var token = dbManager.getUserToken(key);
        Assert.assertNull(token);
    }

    @Test
    public void testAddUserToken() {
        try {
            var key = new TokenKey();
            dbManager.setUserToken(key, null);

            key.teamId = "TEAM123";
            key.userId = "USER123";
            dbManager.setUserToken(key, "xoxp-token-1234");
        } catch(Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSetBotInfoWithNullTeamId() {
        var bot = new Bot("foo", "bar", "baz");

        dbManager.setBotInfo(null, bot);

        Mockito.verify(jedisMock, Mockito.times(0)).hset(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testSetBotInfoWithNullUserId() {
        var bot = new Bot(null, "bar", "baz");

        dbManager.setBotInfo("qux", bot);

        Mockito.verify(jedisMock, Mockito.times(0)).hset(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testSetBotInfoWithNullToken() {
        var bot = new Bot("foo", null, "baz");

        dbManager.setBotInfo("qux", bot);

        Mockito.verify(jedisMock, Mockito.times(0)).hset(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testSetBotInfoWithNullTeamName() {
        var bot = new Bot("foo", "bar", null);

        dbManager.setBotInfo("qux", bot);

        Mockito.verify(jedisMock, Mockito.times(0)).hset(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testSetBotInfo() {
        var bot = new Bot("foo", "bar", "baz");

        dbManager.setBotInfo("qux", bot);

        Mockito.verify(jedisMock, Mockito.times(1)).hset("bot_tokens", "qux", "{\"token\":\"foo\",\"userId\":\"bar\",\"teamName\":\"baz\"}");
    }
}
