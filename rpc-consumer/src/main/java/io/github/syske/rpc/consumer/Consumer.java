package io.github.syske.rpc.consumer;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import io.github.syske.rpc.common.annotation.RpcClient;
import io.github.syske.rpc.common.annotation.RpcComponentScan;
import io.github.syske.rpc.common.annotation.RpcConsumer;
import io.github.syske.rpc.common.proccess.ClassScanner;
import io.github.syske.rpc.common.proccess.RpcClientContentHandler;
import io.github.syske.rpc.common.util.RedisUtil;
import io.github.syske.rpc.common.util.entity.RpcRegisterEntity;
import io.github.syske.rpc.facade.HelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: syske-rpc-server
 * @description: 消费者端
 * @author: syske
 * @date: 2021-06-15 19:34
 */
@RpcConsumer
@RpcComponentScan("io.github.syske.rpc.consumer")
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    @RpcClient
    private HelloService helloService;

    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        initServiceConsumer();
        Map<Class, Object> rpcClientContentMap = RpcClientContentHandler.getRpcClientContentMap();
        Consumer consumer = (Consumer)rpcClientContentMap.get(Consumer.class);
        String syske = consumer.helloService.sayHello("syske");
        logger.info("消费者远程调用返回结果：" + syske);
    }

    private static void initServiceConsumer() throws UnknownHostException {
        final String CONSUMER_KEY = "%s:consumer";
        ClassScanner.init(Consumer.class);
        Set<Class> classSet = ClassScanner.getClassSet();
        String host = InetAddress.getLocalHost().getHostAddress();
        classSet.forEach(c -> {
            Field[] declaredFields = c.getDeclaredFields();
            for (Field field : declaredFields) {
                RpcClient annotation = field.getAnnotation(RpcClient.class);
                if (Objects.nonNull(annotation)) {
                    Class<?> fieldType = field.getType();
                    String name = fieldType.getName();
                    RpcRegisterEntity rpcRegisterEntity = new RpcRegisterEntity();
                    rpcRegisterEntity.setHost(host).setServiceFullName(c.getName());
                    RedisUtil.record2Cache(String.format(CONSUMER_KEY, name), JSON.toJSONString(rpcRegisterEntity));
                    Object proxyInstance = getProxyInstance(fieldType);
                    try {
                        Object consumer = c.newInstance();
                        field.set(consumer, proxyInstance);
                        RpcClientContentHandler.initRpcClientContent(c, consumer);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 获取动态代理实例
     * @param tClass
     * @param <T>
     * @return
     */
    private static <T> T getProxyInstance(Class<T> tClass) {
        return (T)Proxy.newProxyInstance(tClass.getClassLoader(),
                new Class[] {tClass}, new ConsumerProxyInvocationHandler(tClass));
    }

}
