package io.github.syske.rpc.provider;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * @program: syske-rpc-server
 * @description: 服务提供者
 * @author: syske
 * @date: 2021-06-15 19:23
 */
public class Provider {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(8888);
            while (true) {
                Socket accept = serverSocket.accept();
                ObjectInputStream objectInputStream = new ObjectInputStream(accept.getInputStream());
                // 读取类名
                String classFullName = objectInputStream.readUTF();
                // 读取方法名
                String methodName = objectInputStream.readUTF();
                // 读取方法调用入参
                Object[] parameters = (Object[])objectInputStream.readObject();
                // 读取方法入参列表
                Class<?>[] parameterTypes = (Class<?>[])objectInputStream.readObject();
                System.out.println(String.format("收到消费者远程调用请求：类名 = {%s}，方法名 = {%s}，调用入参 = %s，方法入参列表 = %s",
                        classFullName, methodName, Arrays.toString(parameters), Arrays.toString(parameterTypes)));
                Class<?> aClass = Class.forName(classFullName);
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
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
