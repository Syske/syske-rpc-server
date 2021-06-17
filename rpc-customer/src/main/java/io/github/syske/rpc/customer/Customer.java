package io.github.syske.rpc.customer;

import com.alibaba.fastjson.JSON;
import io.github.syske.rpc.common.annotation.RpcClient;
import io.github.syske.rpc.common.annotation.RpcComponentScan;
import io.github.syske.rpc.common.annotation.RpcCustomer;
import io.github.syske.rpc.common.proccess.ClassScanner;
import io.github.syske.rpc.common.util.RedisUtil;
import io.github.syske.rpc.common.util.entity.RpcRegisterEntity;
import io.github.syske.rpc.facade.HelloService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Objects;
import java.util.Set;

/**
 * @program: syske-rpc-server
 * @description: 消费者端
 * @author: syske
 * @date: 2021-06-15 19:34
 */
@RpcCustomer
@RpcComponentScan("io.github.syske.rpc.customer")
public class Customer {

    @RpcClient
    private HelloService helloService;

    public static void main(String[] args) {
        try {
            ClassScanner.init(Customer.class);
            initServiceCustomer();
            String serviceFacade = "io.github.syske.rpc.service.HelloSercieImpl";
            String methodName = "sayHello";
            String[] parameters = {"syske"};
            Class<?>[] parameterTypes = {String.class};
            Socket socket = new Socket("127.0.0.1", 8889);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            // 写类名
            objectOutputStream.writeUTF(serviceFacade);
            // 写参数
            objectOutputStream.writeObject(parameters);
            // 读取返回值
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            Object o = objectInputStream.readObject();
            System.out.println("消费者远程调用返回结果：" + o);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sayHi() {
        System.out.println(helloService.sayHello("syske"));
    }


    private static void initServiceCustomer() {
        final String CUSTOMER_KEY = "%s:customer";
        final String PROVIDER_KEY = "%s:provider";
        Set<Class> classSet = ClassScanner.getClassSet();
        classSet.forEach(c -> {
            Field[] declaredFields = c.getDeclaredFields();
            for (Field field : declaredFields) {
                try {
                    RpcClient annotation = field.getAnnotation(RpcClient.class);
                    if (Objects.nonNull(annotation)) {
                        Class<?> fieldType = field.getType();
                        String name = fieldType.getName();
                        RedisUtil.record2Cache(String.format(CUSTOMER_KEY, name), c.getName());
                        String serviceObject = RedisUtil.getObject(String.format(PROVIDER_KEY, name));
                        RpcRegisterEntity rpcRegisterEntity = JSON.parseObject(serviceObject, RpcRegisterEntity.class);
                        field.set(c.newInstance(), rpcRegisterEntity.getNewInstance());
                    }
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private static void doPoxy(Class<?> tClass, Method method) {
        final String PROVIDER_KEY = "%s:provider";

    }
}
