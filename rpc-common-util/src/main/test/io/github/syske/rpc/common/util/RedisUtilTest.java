package io.github.syske.rpc.common.util;

import org.junit.Test;

/**
 * @author sysker
 * @version 1.0
 * @date 2021-06-16 22:47
 */
public class RedisUtilTest {
    @Test
    public void record2CacheTest() {
        RedisUtil.record2Cache("test", "hello redis");
    }

}
