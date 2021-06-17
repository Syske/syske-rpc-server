package io.github.syske.rpc.common.proccess;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.syske.rpc.common.annotation.RpcComponentScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 类扫描器
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-16 23:08
 */
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
