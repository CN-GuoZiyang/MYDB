package top.guoziyang.mydb.client;

import java.util.Scanner;


/**
* 改用Netty客户端
* 作者：RioAngele
* 时间：2023.5.23
*/
public class Shell {
    private NettyClient client;

    public Shell(NettyClient client) {
        this.client = client;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        try {
            while(true) {
                System.out.print(":> ");
                String statStr = sc.nextLine();
                if("exit".equals(statStr) || "quit".equals(statStr)) {
                    break;
                }
                try {
                    byte[] res = client.execute(statStr.getBytes());
                    System.out.println(new String(res));
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                }

            }
        } finally {
            sc.close();
            client.close();
        }
    }
}
