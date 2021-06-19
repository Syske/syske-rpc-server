package io.github.syske.rpc.common.proccess;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 服务注册处理
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-16 22:37
 */
public class RpcClientContentHandler {
    private static Map<Class, Object> rpcClientContentMap = Maps.newHashMap();

    public static void initRpcClientContent(Class zlass, Object newInstance) {
        if (rpcClientContentMap.containsKey(zlass)) {
            return;
        }
        rpcClientContentMap.put(zlass, newInstance);
    }

    public static Map<Class, Object> getRpcClientContentMap() {
        return rpcClientContentMap;
    }
}
