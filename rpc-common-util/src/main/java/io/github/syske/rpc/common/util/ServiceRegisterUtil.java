package io.github.syske.rpc.common.util;

import com.alibaba.fastjson.JSON;
import io.github.syske.rpc.common.util.entity.RpcRegisterEntity;

/**
 * 服务注册工具类
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-20 10:14
 */
public class ServiceRegisterUtil {
    private static final String ZNODE = "/syske-rpc-server";
    private static final String PROVIDER_PATH = "%s/%s/provider";
    private static final String CONSUMER_PATH = "%s/%s/consumer";

    public static void registerProvider(RpcRegisterEntity entity) {

        ZooKeeperUtil.writeData(String.format(PROVIDER_PATH, ZNODE, entity.getInteraceClassFullName()) , JSON.toJSONString(entity));
    }

    public static void registerConsumer(RpcRegisterEntity entity) {
        ZooKeeperUtil.writeData(String.format(CONSUMER_PATH, ZNODE, entity.getInteraceClassFullName()), JSON.toJSONString(entity));
    }

    public static <T> T getProviderData(String path) {
        return ZooKeeperUtil.readData(String.format(PROVIDER_PATH, ZNODE, path));
    }

    public static <T> T getConsumererData(String path) {
        return ZooKeeperUtil.readData(String.format(CONSUMER_PATH, ZNODE, path));
    }
}
