# RPC服务注册简单实现

### 前言

昨天我们手写了一个简单到不能再简单的`rpc`服务，对`rpc`服务有了一个基本的认知，但昨天的实现太过简单，甚至都算不上`rpc`，因为`rpc`服务的核心是动态代理，但是今天我想先实现`rpc`的注册，今天的服务注册我没有用`zk`，而是`redis`，用`redis`的目的就是让各位小伙伴都能真正明白，任何组件的选用都不是必须的，而是一种更优的选择。

### 服务注册

首先，我们要定义以下几个注解，这些注解的作用就是辅助我们完成服务的注册

#### 定义注解

第一个注解和我们`syske-boot`中的注解作用一致，主要是为了扫描类

```java
/**
 * rpc扫描注解
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-16 23:15
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcComponentScan {
    String[] value();
}
```

这个注解是标记我们的服务提供者，方便我们针对服务提供者进行注册操作

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcProvider {
}
```

然后就是服务消费者，和服务提供者的注解类似，就是为了标记消费者

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcConsumer {
}
```

最后一个注解是加在属性上的，主要是为了方便后期实现动态代理

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcClient {
}
```

#### 包扫描

我们这里直接就把之前写的包扫描器直接用起来了，这里会根据`RpcComponentScan`注解指定的包名进行扫描

```java
public class ClassScanner {
    private static final Logger logger = LoggerFactory.getLogger(ClassScanner.class);

    private static Set<Class> classSet = Sets.newHashSet();

    private ClassScanner() {
    }

    public static Set<Class> getClassSet() {
        return classSet;
    }

    /**
     * 类加载器初始化
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static void init(Class aClass) {
        try {
            // 扫描包
            componentScanInit(aClass);
        } catch (Exception e) {
            logger.error("ClassScanner init error: ", e);
        }
    }

    /**
     * 扫描指定的包路径，如果无该路径，则默认扫描服务器核心入口所在路径
     *
     * @param aClass
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void componentScanInit(Class aClass) throws IOException, ClassNotFoundException {
        logger.info("componentScanInit start init……");
        logger.info("componentScanInit aClass: {}", aClass);
        Annotation annotation = aClass.getAnnotation(RpcComponentScan.class);
        if (Objects.isNull(annotation)) {
            Package aPackage = aClass.getPackage();
            scanPackage(aPackage.toString(), classSet);
        } else {
            String[] value = ((RpcComponentScan) annotation).value();
            for (String s : value) {
                scanPackage(s, classSet);
            }
        }
        logger.info("componentScanInit end, classSet = {}", classSet);
    }

    /**
     * 扫描指定包名下所有类，并生成classSet
     *
     * @param packageName
     * @param classSet
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void scanPackage(String packageName, Set<Class> classSet)
            throws IOException, ClassNotFoundException {
        logger.info("start to scanPackage, packageName = {}", packageName);
        Enumeration<URL> classes = ClassLoader.getSystemResources(packageName.replace('.', '/'));
        while (classes.hasMoreElements()) {
            URL url = classes.nextElement();
            File packagePath = new File(url.getPath());
            if (packagePath.isDirectory()) {
                File[] files = packagePath.listFiles();
                for (File file : files) {
                    String fileName = file.getName();
                    if (file.isDirectory()) {
                        String newPackageName = String.format("%s.%s", packageName, fileName);
                        scanPackage(newPackageName, classSet);
                    } else {
                        String className = fileName.substring(0, fileName.lastIndexOf('.'));
                        String fullClassName = String.format("%s.%s", packageName, className);
                        classSet.add(Class.forName(fullClassName));
                    }
                }
            } else {
                String className = url.getPath().substring(0, url.getPath().lastIndexOf('.'));
                String fullClassName = String.format("%s.%s", packageName, className);
                classSet.add(Class.forName(fullClassName));
            }
        }
    }
}
```

目的就是扫描我们的服务提供者，方便注册服务的时候使用。

#### 服务提供者注册

首先我们要先通过`RpcProvider`注解拿到我们的服务提供者，然后组装我们的的注册信息。

```java
    private static void initServiceProvider() {
        Set<Class> classSet = ClassScanner.getClassSet();
        classSet.forEach(c -> {
            Annotation annotation = c.getAnnotation(RpcProvider.class);
            if (Objects.nonNull(annotation)) {
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
            }            
        });
    }
```

注册信息注册完成后，我们将注册信息写入`redis`。

#### 服务消费者注册

消费者注册也是类似的方法

```java
private static void initServiceConsumer() {
        final String CONSUMER_KEY = "%s:consumer";
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
                        RedisUtil.record2Cache(String.format(CONSUMER_KEY, name), c.getName());
                        // String serviceObject = RedisUtil.getObject(String.format(PROVIDER_KEY, name));
                        // RpcRegisterEntity rpcRegisterEntity = JSON.parseObject(serviceObject, RpcRegisterEntity.class);
                        // field.set(c.newInstance(), rpcRegisterEntity.getNewInstance());
                    }
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }
```

本来打算在消费者注册完直接给接口赋值的（注释部分），但是在实际操作的时候，发现这种方式行不通，因为服务提供者和消费者是不同的应用，序列化之后的服务提供者的实例（`rpcRegisterEntity.getNewInstance()`）是没法强转的，控制台一直会报类型不匹配的错误：

![](https://gitee.com/sysker/picBed/raw/master/images/20210617130331.png)

而且，后来我想了下，如果这种方式真的实现了，那就没`socket`什么事了，还能叫`rpc`吗？所以要想实现，真正的动态调用，还是要通过动态代理。

这么说来，昨天实现的也不能叫`rpc`，因为没有实现动态代理。

#### 测试

分别运行服务提供者和消费者，然后我们去`redis`看下，服务是否已经注册上，如果看到如下所示，表面服务已经成功注册：

![](https://gitee.com/sysker/picBed/raw/master/images/20210617130907.png)

同时，我发现服务提供者的实例信息是空的，着有进一步表明，这种直接反射赋值方式行不通，只有动态代理才能拯救我们的`rpc`服务。

![](https://gitee.com/sysker/picBed/raw/master/images/20210617131121.png)

### 总结

不得不说，相比于`zk`，`redis`确实不适合做服务注册，毕竟`zk`的树形结构看起来就很友好，但是我暂时不考虑换成`zk`，等动态代理实现了再说。

另外，在实际测试中，我发现除了`classFUllName`之外，其他的参数都是冗余的，但是像服务的地址、端口等比较重要的信息又没有，所以后面要把服务注册的`entity`优化下，暂时就先这样。

明天，我明天打算分享动态代理的实现过程，这一块实现了，`rpc`框架就成了，具体的明天再说，好了，今天就到这里吧！
