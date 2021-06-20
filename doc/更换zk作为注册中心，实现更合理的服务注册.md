# 更换zk作为注册中心，实现更合理的服务注册

### 前言

昨天我们对手写的`rpc`框架进行了整理优化，解决了代理类和`sevice`的绑定问题，自此之后我们就无需再进行手动绑定，确实方便了很多。

今天，我考虑把我们的`rpc`框架服务注册中心改为`zk`，至于为什么要改，原因很简单，`redis`严格来说属于数据库，对于多节点数据存放确实不方便也不够灵活，但是`zk`就是专门用来存储多节点数据的树形结构，不仅可以存放`k-v`这样的数据，同时还有父子节点这样的数据关系，确实比`redis`更灵活。

所以，我考虑把注册中心替换成`zk`，当然另外一个原因是，我也想更深入的了解下`zk`，而不是仅仅停留在会用的层面。之前一直有用`zk`作为服务注册中心，但也一直停留在使用层面，对于其内部原理一直是一知半解，直到今天真正用注册我们之前手写的`rpc`服务，踩了好多坑，才发现`zk`方面确实该好好补课了。

### 整合zk

#### 引入客户端依赖

开始之前，我们先要引入`zk`的客户端，我引入的是一个第三方包，这个客户端是基于官方的客户端开发的，也算比较主流。

```xml
<dependency>
    <groupId>com.101tec</groupId>
    <artifactId>zkclient</artifactId>
    <version>0.11</version>
</dependency>
```

本来是打算直接使用官方客户端的，但是在使用的时候，发现确实不太好用，考虑到时间，最后选择了这个第三方客户端。后面有时间的话，考虑自己写一个`zk`客户端，目前考虑的是基于官方的客户端实现，这也算是研究学习`zk`的一种方式。



#### 编写zk客户端工具类

然后编写`zk`工具类，目前的工具类还比较简单，而且有一些问题，比如节点创建不够灵活，目前都是写死的，只能创建三层目录的节点，例如`/syske-rpc-server/io.github.syske.rpc.facade.HelloService/consumer`，一部分原因是目前我对这个客户端了解的也比较少，所以也就用了一些简单的接口方法。

后面需要再优化下，考虑通过递归实现多层目录自动创建节点。

```java
public class ZooKeeperUtil {
    private static final String ZK_ADDRESS = "127.0.0.1:2181";

    private static final String ZNODE = "/syske-rpc-server";

    private static ZkClient client;

    static {
        client =  new ZkClient(ZK_ADDRESS);
    }

    public static void writeData(String path, String data) {
        if (!client.exists(ZNODE)){
            //创建持久化节点 ,初始化数据
            String[] paths = path.split("/");
            client.createPersistent(ZNODE, "/" + paths[1]);
            client.createPersistent(ZNODE + "/" + paths[1], "/" + paths[2]);
            String chlid = client.create(ZNODE + path, data, CreateMode.PERSISTENT);
            System.out.println(chlid);
        }else {
            //修改节点数据,并返回该节点的状态
            String[] paths = path.split("/");
            client.createPersistent(ZNODE + path);
            Stat znodeStat = client.writeDataReturnStat(ZNODE + path, data, -1);
            System.out.println(znodeStat);
        }
    }

    public static <T> T readData(String path) {
        //获取节点数据
        return client.readData(ZNODE + path);
    }
}
```

#### 服务注册

编写服务注册工具类，这个工具类主要是用于服务注册和获取服务注册信息。

```java
public class ServiceRegisterUtil {

    public static void registerProvider(RpcRegisterEntity entity) {
        ZooKeeperUtil.writeData(String.format("/%s/provider", entity.getServiceFullName()) , JSON.toJSONString(entity));
    }

    public static void registerConsumer(RpcRegisterEntity entity) {
        ZooKeeperUtil.writeData(String.format("/%s/consumer", entity.getServiceFullName()), JSON.toJSONString(entity));
    }

    public static <T> T getProviderData(String path) {
        return ZooKeeperUtil.readData(String.format("/%s/provider", path));
    }

    public static <T> T getConsumererData(String path) {
        return ZooKeeperUtil.readData(String.format("/%s/consumner", path));
    }
}
```

#### 优化注册方式

然后把我们之前用`redis`注册代码改成我们刚写的工具类即可：

##### 服务提供者

替换注册方法

![](https://gitee.com/sysker/picBed/raw/master/images/20210620164412.png)

##### 服务消费者

替换注册方法

![](https://gitee.com/sysker/picBed/raw/master/images/20210620165347.png)

同时还有我们动态代理远程调用部分的代码，这里只需要替换获取方法即可：

![](https://gitee.com/sysker/picBed/raw/master/images/20210620165539.png)

#### 测试

然后我们分别启动服务提供者和服务消费者，看下效果，首先看下`zookeeper`服务注册情况：

![](https://gitee.com/sysker/picBed/raw/master/images/20210620165954.png)

看了上面的注册结构是不是很熟悉呢？`dubbo`的服务注册不就张这样吗？

然后我们启动消费者端远程调用下看下：

![](https://gitee.com/sysker/picBed/raw/master/images/20210620170203.png)

先从`zk`中获取了服务的注册信息，然后远程调用，结果正常返回，是不是很简单呢？

### 总结

基于`zk`的服务注册，我们已经完整地实现了，虽然有很多地方都需要进一步的优化，但是整体流程是`ok`的，而且实际实现过程中，确实也发现了，对于`zk`我以前是真的只会用，数据结构都没好好研究过，后面得好好补课了。

另外，截至目前，除了数据结构方面的优势，我并没有发现`zk`作为服务注册中心的其他的优势，当然也有可能是我对`zk`了解的不够深入，后面我会通过手写`zk`客户端来弥补这一块知识的不足。

不过，至少目前说明仅作为服务注册中心而言（简单应用），`zk`确实没有比`redis`强多少，所以说，在没有经过确切的实践论证前，就简单地认为两种技术之间存在着必然地联系，是不科学的，用教员的话说，就是实践是检验真理的唯一方式。而且实际应用中，特别是在在早期应用中，有好多企业其实就是将服务的注册信息写在数据库中的，然后在服务启动时候去获取服务地址，我上一家单位就是这么搞的，每次发布程序的时候，都需要执行数据单，是真的麻烦……好了，今天就到这里吧！

