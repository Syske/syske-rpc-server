package io.github.syske.rpc.common.util.entity;

import java.io.Serializable;
import java.util.Arrays;

/**
 * 服务注册域对象
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-16 22:17
 */
public class RpcRegisterEntity implements Serializable {
    /**
     * 全类名
     */
    private String classFullName;
    /**
     * 方法名
     */
    private String methodName;

    /**
     * 方法参数列表
     */
    private Class<?>[] parameterTypes;

    private Object newInstance;

    public RpcRegisterEntity() {
    }

    public RpcRegisterEntity(String classFullName, String methodName, Class<?>[] parameterTypes, Object newInstance) {
        this.classFullName = classFullName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.newInstance = newInstance;
    }

    public String getClassFullName() {
        return classFullName;
    }

    public void setClassFullName(String classFullName) {
        this.classFullName = classFullName;
    }

    public String getMethodName() {
        return methodName;
    }

    public RpcRegisterEntity  setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public RpcRegisterEntity setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
        return this;
    }

    public Object getNewInstance() {
        return newInstance;
    }

    public void setNewInstance(Object newInstance) {
        this.newInstance = newInstance;
    }

    @Override
    public String toString() {
        return "RpcRegisterEntity{" +
                "classFullName='" + classFullName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                '}';
    }
}
