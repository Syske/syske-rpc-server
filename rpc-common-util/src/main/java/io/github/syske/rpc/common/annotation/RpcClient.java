package io.github.syske.rpc.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * rpc客户端注解，即消费者
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-17 0:16
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcClient {
}
