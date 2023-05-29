package top.guoziyang.mydb.client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import top.guoziyang.mydb.transport.Encoder;
import top.guoziyang.mydb.transport.Packager;
import top.guoziyang.mydb.transport.Transporter;


/**
* 改用Netty客户端
* 作者：RioAngele
* 时间：2023.5.23
*/
public class Launcher {
    public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
//        Socket socket = new Socket("127.0.0.1", 9999);
//        Encoder e = new Encoder();
//        Transporter t = new Transporter(socket);
//        Packager packager = new Packager(t, e);

//        Client client = new Client(packager);
        NettyClient client=new NettyClient();
        Shell shell = new Shell(client);
        shell.run();
    }
}
