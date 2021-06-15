package io.github.syske.rpc.facade;

/**
 * @program: syske-rpc-server
 * @description: hello服务
 * @author: syske
 * @date: 2021-06-15 19:52
 */
public interface HelloService {
    /**
     * say hello
     * @param name
     * @return
     */
    String sayHello(String name);
}
