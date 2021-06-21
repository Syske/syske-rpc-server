# 优化zk注册工具类，完善注册机制

### 前言

昨天，我们把手写的`rpc`服务的注册中心改成`zookeeper`，但是在今天测试的时候，我发现有一些问题，一个问题就是我们昨天说的问题，节点创建的时候，不够灵活；另一个问题，就是昨天代码里面还残留了一部分`redis`的代码，所以实际上服务提供者查询服务信息依然调用的是`reids`，今天我们就来优化下这两个问题。

### 问题优化

#### zk工具类优化

不能直接创建节点的问题，我发现是我没有用对方法，其实在`zKClient`中提供了`createPersistent(String path, boolean createParents)`，这个方法如果后面的布尔值表示是否创建父节点，如果值为`true`，且父级节点不存在，它会自动帮我们创建，如果节点已经存在，它会自动跳过创建，只会写入节点数据，这样我们昨天节点创建的问题也解决了。

优化之后的`ZooKeeperUtil`：

```java
public class ZooKeeperUtil {
    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperUtil.class);
    private static final String ZK_ADDRESS = "127.0.0.1:2181";

    private static ZkClient client;

    static {
        client = new ZkClient(ZK_ADDRESS);
    }

    public static void writeData(String path, String data) {
        if (!client.exists(path)) {
            // 创建持久化节点 ,初始化数据
            client.createPersistent(path, true);
        }
        // 修改节点数据,并返回该节点的状态
        client.writeData(path, data, -1);

    }

    public static <T> T readData(String path) {
        // 获取节点数据
        return client.readData(path);
    }

}
```

#### 服务提供者获取注册信息问题优化

今天在优化测试的时候，没有启动`reids`，然后一直报`connection reset`，我以为是端口被占用了，但是改了端口还是没用，最后才发现是获取服务注册信息那块还用的是`redis`，测试过程中，又发现服务提供者的注册信息还少了服务实现的类型信息，同时也发现动态代理的时候会调用`toString`方法，但是服务消费者并没有调用记录，服务提供者也不打印，这块我还挺困惑的，后面再研究下。

优化后的注册实体：

```java
public class RpcRegisterEntity implements Serializable {
    /**
     * 对服务消费者，该值表示调用方全类名；对服务提供者而言，该值表示接口实现全类名
     */
    private String interaceClassFullName;

    /**
     * 接口Ip
     */
    private String host;

    /**
     * 接口端口号
     */
    private int port;
    /**
     * 服务提供者实现类名
     */
    private String serviceImplClassFullName;
 }
```

只是增加了`serviceImplClassFullName`属性，主要是方便服务提供者拿到实现类的信息，方便方法调用。

然后是服务提供者的优化：

![](https://gitee.com/sysker/picBed/raw/master/images/20210621125819.png)

之前一直是从`redis`中获取的，`redis`中的`interaceClassFullName`一直是服务提供者的实现类的类名，现在改了之后，才是合理的。

#### 测试

优化完上面的问题后，我们运行测试下。

##### 服务提供者

![](https://gitee.com/sysker/picBed/raw/master/images/20210621131002.png)

##### 服务消费者

![](https://gitee.com/sysker/picBed/raw/master/images/20210621131430.png)

结果很完美。

### 总结

优化解决问题一定要找到问题的关键，但同时要不断试错，只有如此，你才能积累更多经验，然后才能更快地排查出问题。

到这里，我们整个手写`rpc`服务暂时告一段落，后面的话，我们会继续分享其他的技术和解决方案，如果哪天突然有了好的想法，保不准还是会拿我们的`rpc`开刀。

关于后续内容更新，目前考虑的还是继续通过手写组件这样的方式，一点一点深入了解各种新的技术和框架，这个过程我个人觉得还是蛮有意思的，好了，今天就到这里吧。





























