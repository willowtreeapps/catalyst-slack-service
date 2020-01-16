package db;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import util.MockConfig;

public class RedisDbManagerTest {

    RedisDbManager dbManager;

    @Before
    public void setup() {
        var jedisPool = Mockito.mock(JedisPool.class);
        var jedis = Mockito.mock(Jedis.class);
        dbManager = new RedisDbManager(new MockConfig());
        dbManager._jedisPool = jedisPool;
        Mockito.when(jedisPool.getResource()).thenReturn(jedis);
        Mockito.when(jedis.hget("user_tokens", "TEAM123_USER123")).thenReturn("xoxp-token-1234");
    }

    @Test
    public void testUpdateMessageCounts() {
        try {
            dbManager.updateMessageCounts(null, null);
            dbManager.updateMessageCounts("TEAM123", "USER123");
        } catch(Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetUserToken() {
        var token = dbManager.getUserToken("TEAM123", "USER123");
        Assert.assertEquals("xoxp-token-1234", token);
    }

    @Test
    public void testAddUserToken() {
        try {
            dbManager.addUserToken(null, null, null);
            dbManager.addUserToken("TEAM123", "USER123", "xoxp-token-1234");
        } catch(Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
