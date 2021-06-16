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

这里我通过`objectInputStream`和`objectOutputStream`进行数据操作，因为我们操作的都是`java`的`object`，所以用这个就比较方便，当然网上绝大多示例也都是这么实现的。

你直接用`InputStream/OutputStream`也是可以的，重要的是要理解原理。

这里需要注意的是，服务端读取的顺序，必须和客户端发送的一致，否则在反序列化的时候会报错，会强转失败:

1. 读取类名
2. 读取方法名
3. 读取参数
4. 读取参数类型

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
                // 读取方法入参类型
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

##### 接口实现

这里的接口实现就是我们提供给消费者的服务。

```java
public class HelloSercieImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "hello, " + name;
    }
}
```



##### 服务消费者

前面说了，服务消费者发送消息的顺序必须和服务提供者读取的顺序一致，所以消费者发送的顺序也必须如下：

1. 写类名
2. 写方法名
3. 写参数
4. 写参数类型

```java
public static void main(String[] args) {
        try {
            String classFullName = "io.github.syske.rpc.service.HelloSercieImpl";
            String methodName = "sayHello";
            String[] parameters = {"syske"};
            Class<?>[] parameterTypes = {String.class};
            Socket socket = new Socket("127.0.0.1", 8888);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeUTF(classFullName);
            objectOutputStream.writeUTF(methodName);
            objectOutputStream.writeObject(parameters);
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
```

这里的方法名和参数列表主要是为了确认方法的唯一性，便于获取方法，后期如果引入`facade`层，方法名和参数可以直接从接口获取。

#### 测试

我们来测试一下，先启动服务提供者，然后启动服务消费者，这时候会看到服务提供者控制台输出如下信息：

![](https://gitee.com/sysker/picBed/raw/master/images/20210616125400.png)

同时，消费者控制台也返回了远程调用结果：

![](https://gitee.com/sysker/picBed/raw/master/images/20210616125445.png)

到这里，我们简易的`rpc`服务框架就基本完成了。怎么样，是不是很简单呢？

### 总结

不知道大家发现没，虽然我们的`rpc`框架实现了，但是在调用具体方法的时候很不灵活，不仅需要执行实现类，要指定方法名、入参、参数列表，同时还需要配置服务地址和端口，很繁琐，而且如果我们的远程接口很多，我们就需要配置很多不同的接口信息，接口管理起来就很不方便。

所以这时候我们就应该明白了，为什么`rpc`框架需要注册中心，以及注册中心的作用？是的，它就是用来注册管理我们的服务的，让我们可以更方便地找到服务的提供者以及接口的信息。

我现在对很多框架了组件有了更深刻的了解和认知，任何组件的出现都是为了解决具体的问题，比如`zookeeper`、`redis`、配置中心等，但这些组件并非必须的，所以我们在使用具体组件的时候，已经要有知其然，知其所以然的思维，不能仅仅停留在会用的层面，而应该去思考为什么用，为什么不用，只有在这样的思维指导下，你才可能在这个行业走的更远，至少作为一名技术人员，我觉得是这样的。

技术本身只是器，只是工具，脱离技术的思维才是`yyds`，当然行动也是很重要的，我们也需要不断地实践和探索。

和`web`服务器一样，我以前对`rpc`的认知也比较浅显，总觉得这些东西很牛逼，但是当我真正理解了原理，然后自己动手做的时候，我觉得也没有那么难，还是那句话，知易行难，还是要自己动手，才能真正了解其中意。

好了，今天就到这里吧！
