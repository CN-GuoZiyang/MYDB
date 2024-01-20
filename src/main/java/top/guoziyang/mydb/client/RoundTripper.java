package top.guoziyang.mydb.client;

import top.guoziyang.mydb.transport.Package;
import top.guoziyang.mydb.transport.Packager;

public class RoundTripper {
    private Packager packager;

    public RoundTripper(Packager packager) {
        this.packager = packager;
    }
    //发送数据
    //返回得到的结果
    //实现单次收发的功能
    public Package roundTrip(Package pkg) throws Exception {
        packager.send(pkg);
        return packager.receive();
    }

    public void close() throws Exception {
        packager.close();
    }
}
