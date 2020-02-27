package org.catalyst.slackservice.db;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisDbHandlerTest {

    RedisDbHandler dbManager;

    @Before
    public void setup() {
        var jedisPool = Mockito.mock(JedisPool.class);
        var jedis = Mockito.mock(Jedis.class);
        dbManager = new RedisDbHandler(jedisPool);
        Mockito.when(jedisPool.getResource()).thenReturn(jedis);
        Mockito.when(jedis.hget("user_tokens", "TEAM123_USER123")).thenReturn("xoxp-token-1234");
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

}
