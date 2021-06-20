package io.github.syske.rpc.common.util;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * zk工具类
 *
 * @author sysker
 * @version 1.0
 * @date 2021-06-20 9:40
 */
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
            if (!client.exists(ZNODE + path)) {
                client.createPersistent(ZNODE + path);
            }
            Stat znodeStat = client.writeDataReturnStat(ZNODE + path, data, -1);
            System.out.println(znodeStat);
        }
    }

    public static <T> T readData(String path) {
        //获取节点数据
        return client.readData(ZNODE + path);
    }

    public static void main(String[] args) {
        //创建zookeeper连接
        ZkClient client = new ZkClient(ZK_ADDRESS);
        //判断节点是否存在
        if (!client.exists(ZNODE)){
            //创建持久化节点 ,初始化数据
            client.createPersistent(ZNODE,"zkclient");
            String chlid = client.create(ZNODE + "/chlid", "chlid", CreateMode.PERSISTENT);
            System.out.println(chlid);
        }else {
            //修改节点数据,并返回该节点的状态
            Stat znodeStat = client.writeDataReturnStat(ZNODE, "znode", -1);
            System.out.println(znodeStat);
        }
        //获取子节点  子节点:chlid
        List<String> children = client.getChildren(ZNODE);
        children.forEach((String node) -> {
            System.out.println("子节点:" + node);
        });
        //获取节点数据
        String readData = client.readData(ZNODE);
        Stat stat = new Stat();
        //获取节点数据时更新节点状态
        String o = client.readData(ZNODE + "/chlid", stat);
        System.out.println(readData);
        System.out.println(o);
    }


}
