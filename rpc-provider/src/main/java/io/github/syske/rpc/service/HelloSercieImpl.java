package io.github.syske.rpc.service;

import io.github.syske.rpc.common.annotation.RpcProvider;
import io.github.syske.rpc.facade.HelloService;

/**
 * @program: syske-rpc-server
 * @description: hello 服务实现
 * @author: syske
 * @date: 2021-06-15 19:57
 */
@RpcProvider
public class HelloSercieImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "hello, " + name;
    }
}
