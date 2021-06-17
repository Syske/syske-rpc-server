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
public @interface RpcCustomer {
}
```

最后一个注解是加在属性上的，主要是为了方便后期实现动态代理

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcClient {
}
```

