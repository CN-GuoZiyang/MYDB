package top.guoziyang.mydb.transport;

import java.net.ServerSocket;
import java.net.Socket;

import org.junit.Test;

import top.guoziyang.mydb.backend.utils.Panic;

public class PackagerTest {
    @Test
    public void testPackager() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket ss = new ServerSocket(10345);
                    Socket socket = ss.accept();
                    Transporter t = new Transporter(socket);
                    Encoder e = new Encoder();
                    Packager p = new Packager(t, e);
                    Package one = p.receive();
                    assert "pkg1 test".equals(new String(one.getData()));
                    Package two = p.receive();
                    assert "pkg2 test".equals(new String(two.getData()));
                    p.send(new Package("pkg3 test".getBytes(), null));
                    ss.close();
                } catch (Exception e) {
                    Panic.panic(e);
                }    
            }
        }).start();
        Socket socket = new Socket("127.0.0.1", 10345);
        Transporter t = new Transporter(socket);
        Encoder e = new Encoder();
        Packager p = new Packager(t, e);
        p.send(new Package("pkg1 test".getBytes(), null));
        p.send(new Package("pkg2 test".getBytes(), null));
        Package three = p.receive();
        assert "pkg3 test".equals(new String(three.getData()));
    }
}
