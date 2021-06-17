package io.github.syske.rpc.common.util.entity;

import java.io.Serializable;

/**
 * 服务注册域对象
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-16 22:17
 */
public class RpcRegisterEntity implements Serializable {
    /**
     * 对服务消费者，该值表示调用方全类名；对服务提供者而言，该值表示接口实现全类名
     */
    private String serviceFullName;

    /**
     * 接口Ip
     */
    private String host;

    /**
     * 接口端口号
     */
    private int port;


    public RpcRegisterEntity() {
    }

    public RpcRegisterEntity(String interfaceFullName, String host, int port) {
        this.serviceFullName = interfaceFullName;
        this.host = host;
        this.port = port;
    }

    public String getServiceFullName() {
        return serviceFullName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
    }

    public String getHost() {
        return host;
    }

    public RpcRegisterEntity setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public RpcRegisterEntity setPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    public String toString() {
        return "RpcRegisterEntity{" +
                "interfaceFullName='" + serviceFullName + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
