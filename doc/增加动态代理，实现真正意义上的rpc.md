# 增加动态代理，实现真正意义上的rpc

### 前言

昨天我们说没有动态代理的`rpc`不是真正的`rpc`，今天我想说，没有动态代理的`rpc`是没有灵魂的。昨天已经说过了，要实现动态代理，实现真正的`rpc`调用，我这人向来都是言出必行，也不怕被打脸，所以今天，我们就来完成我们昨天的`flag`，为我们的`rpc`框架注入灵魂，让它成为真正的`rpc`框架。

### 实现动态代理

开始之前，我们先看下什么是动态代理？

#### 动态代理

代理（`Proxy`）是`jdk1.3`引入的一种解决方案，提供了很多动态静态代理的方式，因为我也是刚接触这一块，所以这里不做过多说明，后期计划好好再研究下代理这块的内容，下面就简单从我个人使用体验来说下动态代理。

动态代理简答来说，就是在你需要用到某个类的时候，由`Proxy`给你动态构建这个类，同时你可以根据自己的需求定制具体方法的实现，反正从这一块，我发现我可以实现`AOP`的一些功能了，具体的我们后面再说。

#### 构建动态代理方法

这里我们使用`Proxy`类的`newProxyInstance`方法来动态构建我们的远程服务，这个类是`java`官方提供的代理类，通过这个类的`newProxyInstance`方法，我们可以对接口进行动态代理，也就是动态的实现。

`newProxyInstance`方法有三个参数，第一个参数是要代理的类的`ClassLoader`，也就是类的类加载器，第二个是代理类要实现的接口，最后一个参数最关键，是代理方法的处理器，我说的`AOP`就是通过这种方式实现的。

传入三个参数，就可以生成我们的代理类了。

```java
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
```

#### 定义动态代理InvocationHandler

前面说最核心的就是`InvocationHandler`，这个`handler`我们可以根据自己的需要定义，需要继承`InvocationHandler`接口，同时需要重写`invoke`方法，这个方法相当于被动态代理类的方法的拦截器，也就是在你调用被代理类的任何方法时，都会调用该方法，所以你需要在这个方法中实现你想要调用的目标方法。

下面是我自己定义的，我在这个方法中实现了远程调用的业务，也就是当我调入远程接口的方法的时候，我其实是通过`invoke`方法远程调用了服务提供者的具体方法，这才是真`RPC`：

```java
public class ConsumerProxyInvocationHandler implements InvocationHandler {
    private final Logger logger = LoggerFactory.getLogger(ConsumerProxyInvocationHandler.class);
    private final String PROVIDER_KEY = "%s:provider";
    /**
     * 代理类的class
     */
    private Class<?> serviceClass;

    public ConsumerProxyInvocationHandler(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        logger.info("proxy: {}", proxy.getClass().getName());
        logger.info("method: {}", method);
        String interfaceName = serviceClass.getName();
        String serviceObject = RedisUtil.getObject(String.format(PROVIDER_KEY, interfaceName));
        RpcRegisterEntity rpcRegisterEntity = JSON.parseObject(serviceObject, RpcRegisterEntity.class);
        logger.info("args: {}", Arrays.toString(args));
        Socket socket = new Socket("127.0.0.1", 8889);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        // 写接口类名
        objectOutputStream.writeUTF(interfaceName);
        // 发送方法名
        objectOutputStream.writeUTF(method.getName());
        // 写方法参数列表
        objectOutputStream.writeObject(method.getParameterTypes());
        // 写调用入参参数
        objectOutputStream.writeObject(args);
        // 读取返回值
        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        logger.info("执行方法前，这里可以实现aop的before需求");
        Object o = objectInputStream.readObject();
        logger.info("执行方法完成，这里可以实现aop的after需求，消费者远程调用返回结果：{}", o);
        return "hello, proxyInvoke, result = " + o;
    }
}
```

`invoke`方法有三个参数，第一个就是我们动态代理生成的代理对象，通过打印他的信息我们发现，他的包名并不是我们接口的包名，而是固定的；第二参数是我们的反射的方法，也就是每次在调用具体方法的时候，会传该方法的反射对象，我们可以通过对反射对象进行业务判断，实现我们的特殊操作；第三个参数是我们调用方法时传入的入参。通过这三个参数，我们可以实现各种特殊需求，比如我前面说的`AOP`。

我上面的实现中，因为直接调用了远程方法，所以我没有太多业务判断，但是我能想到很多应用场景，比如说最常见的一个：你要根据第三方的需求对外提供一部分接口，但是第三方接口有很多，你只需要提供其中的一部分，那你就可以通过动态代理的方式来实现，省去了其他不需要方法的实现；另外一种应用就是我一直在说的`AOP`，据说`Spring`、`Struts`等框架就是通过动态代理技术来实现日志、切面编程这些操作的。

#### 测试

到这里，我们的真`RPC`就改造完了，我们接下来测试下看看吧！

首先启动服务端：

![](https://gitee.com/sysker/picBed/raw/master/images/20210618221121.png)

这里需要说明的是，我们已经把服务注册信息调整过了，现在可以直接从注册中心获取服务提供者的信息了，再也不用写死类名、地址、端口这些信息了

然后启动消费者，调用服务：

![](https://gitee.com/sysker/picBed/raw/master/images/20210618221331.png)

上面的信息表明，我们的`rpc`服务被完美调用成功，同时也表明动态代理是可以实现`AOP`相关操作的。另外，大家注意看下，动态代理生成的代理类的名称是`com.sun.proxy.$Proxy0 `，和其他类最大的区别是它的类名中有`$`，不知道代理包名是否支持自定义，后面再研究下。

### 总结

动态代理这种技术还是很牛逼的，一个是它可以在运行时动态实现接口，这就很厉害了，不仅可以有效解耦，还能实现很多骚操作，比如混淆编译，即编译的时候在代码中混淆一些东西，防止别人反编译，然后在运行时通过远程获取混淆码进行反混淆操作，然后再运行（这是我个人的想法，具体咋实现没研究过）；另一个就是方法拦截器，这个可操作性也很大，比如我说的`AOP`、日志记录等。总之，这个东西属于那种让我相见恨晚的技术，很牛逼，必须赞一个！

今天更新有点晚，主要是今天很忙事情很多：请了一天假，早上准备了一些资料，所以文章写了一半；下午打完了自己人生的第一次诉讼（法庭给我的感觉和自己之前的认知还是有差异的，可能是电视看多了，不过目前一切都很顺序，剩下的就等判决书了）；然后家里人有事出门了，我还要看家照看爷爷，所以做晚饭、吃饭、洗锅，之后才有时间继续写，我个人感觉，写东西这种事，还是一个人的时候效率高。

今天天气不错，心情也还好，同时有一些其他的收获，终于抢到了关注已久的电脑，目前的电脑已经用了九年了，联想小新`16 pro`，关注好久了，本来打算整`RXT3050`，但是一直没货，想着我也不咋打游戏，今天刚好集显有货就买了，这一款电脑关注好久了，整体配置还不错，近期想入手电脑的小伙伴可以关注下，`2.5k`屏、`AMD R7 5800H`处理器、`16G`内存，价格真香，还要啥自行车？

最后，放几张今天拍的美图（一点都没有修图哦），这天空美滋滋呀……

![](https://gitee.com/sysker/picBed/raw/master/images/20210618224941.png)

![](https://gitee.com/sysker/picBed/raw/master/images/20210618225126.png)

![](https://gitee.com/sysker/picBed/raw/master/images/20210618225413.png)

![](https://gitee.com/sysker/picBed/raw/master/images/20210618225459.png)

![](https://gitee.com/sysker/picBed/raw/master/images/20210618225320.png)