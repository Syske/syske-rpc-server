package io.github.syske.rpc.customer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * @program: syske-rpc-server
 * @description: 消费者端
 * @author: syske
 * @date: 2021-06-15 19:34
 */
public class Customer {
    public static void main(String[] args) {
        try {
            String classFullName = "io.github.syske.rpc.service.HelloSercieImpl";
            String methodName = "sayHello";
            String[] parameters = {"syske"};
            Class<?>[] parameterTypes = {String.class};
            Socket socket = new Socket("127.0.0.1", 8889);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            // 写类名
            objectOutputStream.writeUTF(classFullName);
            // 写方法名
            objectOutputStream.writeUTF(methodName);
            // 写参数
            objectOutputStream.writeObject(parameters);
            // 写参数类型
            objectOutputStream.writeObject(parameterTypes);
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
}
