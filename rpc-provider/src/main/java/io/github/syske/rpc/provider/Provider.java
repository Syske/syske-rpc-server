package io.github.syske.rpc.provider;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.Set;

import com.alibaba.fastjson.JSON;

import io.github.syske.rpc.common.annotation.RpcComponentScan;
import io.github.syske.rpc.common.annotation.RpcProvider;
import io.github.syske.rpc.common.proccess.ClassScanner;
import io.github.syske.rpc.common.util.RedisUtil;
import io.github.syske.rpc.common.util.entity.RpcRegisterEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @program: syske-rpc-server
 * @description: 服务提供者
 * @author: syske
 * @date: 2021-06-15 19:23
 */
@RpcComponentScan("io.github.syske.rpc.service")
public class Provider {
    private static final Logger logger = LoggerFactory.getLogger(Provider.class);
    private static final String PROVIDER_KEY = "%s:provider";
    private static final int port = 8889;

    public static void main(String[] args) {
        try {

            ServerSocket serverSocket = new ServerSocket(port);
            ClassScanner.init(Provider.class);
            initServiceProvider();
            while (true) {
                System.out.println("服务提供者已启动，等待连接中……");
                Socket accept = serverSocket.accept();
                ObjectInputStream objectInputStream = new ObjectInputStream(accept.getInputStream());
                // 读取类名
                String interfaceName = objectInputStream.readUTF();
                String methodName = objectInputStream.readUTF();
                // 读取方法名
                Class<?>[] parameterTypes = ( Class<?>[])objectInputStream.readObject();
                // 读取方法调用入参
                Object[] parameters = (Object[])objectInputStream.readObject();
                String serviceObject = RedisUtil.getObject(String.format(PROVIDER_KEY, interfaceName));
                RpcRegisterEntity rpcRegisterEntity = JSON.parseObject(serviceObject, RpcRegisterEntity.class);
                Class<?> aClass = Class.forName(rpcRegisterEntity.getServiceFullName());
                Method method = aClass.getMethod(methodName, parameterTypes);
                Object invoke = method.invoke(aClass.newInstance(), parameters);
                // 回写返回值
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(accept.getOutputStream());
                System.out.println("方法调用结果：" + invoke);
                objectOutputStream.writeObject(invoke);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private static void initServiceProvider() throws UnknownHostException {
        Set<Class> classSet = ClassScanner.getClassSet();
        String host = InetAddress.getLocalHost().getHostAddress();
        classSet.forEach(c -> {
            Annotation annotation = c.getAnnotation(RpcProvider.class);
            if (Objects.nonNull(annotation)) {
                RpcRegisterEntity rpcRegisterEntity = new RpcRegisterEntity(c.getName(), host, port);
                Class[] interfaces = c.getInterfaces();
                String interfaceName = interfaces[0].getName();
                RedisUtil.record2Cache(String.format(PROVIDER_KEY, interfaceName),
                    JSON.toJSONString(rpcRegisterEntity));
                logger.info(JSON.toJSONString(rpcRegisterEntity));
            }
        });
    }
}
