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

    public static void registerProvider(RpcRegisterEntity entity) {
        ZooKeeperUtil.writeData(String.format("/%s/provider", entity.getServiceFullName()) , JSON.toJSONString(entity));
    }

    public static void registerConsumer(RpcRegisterEntity entity) {
        ZooKeeperUtil.writeData(String.format("/%s/consumer", entity.getServiceFullName()), JSON.toJSONString(entity));
    }

    public static <T> T getProviderData(String path) {
        return ZooKeeperUtil.readData(String.format("/%s/provider", path));
    }

    public static <T> T getConsumererData(String path) {
        return ZooKeeperUtil.readData(String.format("/%s/consumner", path));
    }
}
