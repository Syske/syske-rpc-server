package io.github.syske.rpc.common.util;

import redis.clients.jedis.Jedis;

/**
 * redis工具类
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-16 22:39
 */
public class RedisUtil {
    private static Jedis jedis = new Jedis("127.0.0.1", 6379);

    public static void record2Cache(String key, String value) {
        jedis.set(key, value);
    }

    public static String getObject(String key) {
        return jedis.get(key);
    }
}
