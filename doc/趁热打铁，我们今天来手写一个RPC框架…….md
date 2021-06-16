# 趁热打铁，我们今天来手写一个RPC框架……

### 前言

昨天大概理了下思路，觉得目前最合适的事，就是手写一个`rpc`框架，因为只有创造、创作，才让我觉得生活更有激情，而且做具体的事，也会让生活更充实、更真实，会让你不那么迷茫。

如果有同样感受的小伙伴，可以试着找一些具体的事来做一做，这样你也就不再那么浮躁了。

好了，废话少说，我们直接正文。

### 手写RPC

在开始正文前，我们先看下什么是`rpc`。

#### 关于RPC

`rpc`的英文全称是`Remote Procedure Call`，翻译过来就是远程调用，顾名思义，`rpc`就是一套接口的调用方式，一种调用协议。

关于`rpc`的调用原理，这里放上百度百科的一张图片，供大家参考：

![](https://gitee.com/sysker/picBed/raw/master/images/20210616082500.png)

正如上图所示，因为是远程调用，所以接口的调用方和被调用方是通过网络实现调用和通信的，今天我们是基于`socket`来实现远程调用的。

从`rpc`广义的定义角度来说，`restful`也属于远程调用，只是遵循的通信协议不一样。

好了，基础知识就到这里，下面我们看下具体如何实现。

#### 具体实现

开始之前，我想先说下思路。在此之前，我们已经手写过`springboot`的一个简易框架，通过那个框架，我们了解了服务器的工作流程，和调用过程，当然更重要的是也为我们今天的实现提供思路。简单来说，今天的实现是这样的：

分别创建`socket`服务端和客户端，他们分别代表服务提供者和服务消费者，服务提供者先启动（服务端），消费者向服务端发送访问请求（包括类名、方法名、参数等），服务提供者收到接口访问请求时，解析请求参数（拿出类名、方法名、参数列表），通过反射调用方法，并将执行结果返回，然后调用完成。

这里是简单的流程图：

![](https://gitee.com/sysker/picBed/raw/master/images/20210616084450.png)

下面我们看下具体实现过程。

##### 服务提供者

就是一个`socket`服务端，这里最核心的问题是，你需要提前确定消费者和提供者要传输哪些数据，因为客户端也是我们自己写，所以具体数据传输就比较灵活。

这里我通过`objectInputStream`和`objectOutputStream`进行数据操作，因为我们操作的都是`java`的`object`，所以用这个就比较方便，当然主要的原因还是因为网上绝大多都是这么实现的。

你直接用`InputStream/OutputStream`也是可以的，重要的是要理解原理。

这里实现

```java
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
```

