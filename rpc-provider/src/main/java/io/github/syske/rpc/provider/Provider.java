package io.github.syske.rpc.provider;

import com.alibaba.fastjson.JSON;
import io.github.syske.rpc.common.annotation.RpcComponentScan;
import io.github.syske.rpc.common.proccess.ClassScanner;
import io.github.syske.rpc.common.util.RedisUtil;
import io.github.syske.rpc.common.util.entity.RpcRegisterEntity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Set;

/**
 * @program: syske-rpc-server
 * @description: 服务提供者
 * @author: syske
 * @date: 2021-06-15 19:23
 */
@RpcComponentScan("io.github.syske.rpc.service")
public class Provider {
    private static final String PROVIDER_KEY = "%s:provider";

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8889);
            ClassScanner.init(Provider.class);
            initServiceProvider();
            while (true) {
                System.out.println("服务提供者已启动，等待连接中……");
                Socket accept = serverSocket.accept();
                ObjectInputStream objectInputStream = new ObjectInputStream(accept.getInputStream());
                // 读取类名
                String interfaceName = objectInputStream.readUTF();
                // 读取方法调用入参
                Object[] parameters = (Object[])objectInputStream.readObject();
                String serviceObject = RedisUtil.getObject(String.format(PROVIDER_KEY, interfaceName));
                RpcRegisterEntity rpcRegisterEntity = JSON.parseObject(serviceObject, RpcRegisterEntity.class);
                Class<?> aClass = Class.forName(rpcRegisterEntity.getClassFullName());
                Method method = aClass.getMethod(rpcRegisterEntity.getMethodName(), rpcRegisterEntity.getParameterTypes());
                Object invoke = method.invoke(rpcRegisterEntity.getNewInstance(), parameters);
                // 回写返回值
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(accept.getOutputStream());
                System.out.println("方法调用结果：" + invoke);
                objectOutputStream.writeObject(invoke);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static void initServiceProvider() {
        Set<Class> classSet = ClassScanner.getClassSet();
        classSet.forEach(c -> {
            Method[] methods = c.getDeclaredMethods();
            for (Method method : methods) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                String methodName = method.getName();
                try {
                    Object newInstance = c.newInstance();
                    RpcRegisterEntity rpcRegisterEntity = new RpcRegisterEntity(c.getName(), methodName, parameterTypes, newInstance);
                    Class[] interfaces = c.getInterfaces();
                    String interfaceName = interfaces[0].getName();
                    RedisUtil.record2Cache(String.format(PROVIDER_KEY, interfaceName), JSON.toJSONString(rpcRegisterEntity));
                    System.out.println(JSON.toJSONString(rpcRegisterEntity));
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
