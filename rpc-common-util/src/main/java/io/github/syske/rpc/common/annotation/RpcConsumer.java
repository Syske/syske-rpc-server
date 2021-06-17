package io.github.syske.rpc.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于服务消费者注册
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-17 7:54
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcConsumer {
}
