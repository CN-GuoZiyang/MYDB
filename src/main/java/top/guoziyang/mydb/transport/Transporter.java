package top.guoziyang.mydb.transport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class Transporter {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public Transporter(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }
    //将数据转化为16进制
    //然后将数据写入输出流
    public void send(byte[] data) throws Exception {
        String raw = hexEncode(data);
        writer.write(raw);
        writer.flush();
    }
    //接收数据
    public byte[] receive() throws Exception {
        String line = reader.readLine();
        if(line == null) {
            close();
        }
        return hexDecode(line);
    }
    //关闭文件流
    public void close() throws IOException {
        writer.close();
        reader.close();
        socket.close();
    }
    //将字节数组转化为16进制，并且在信息末尾加上换行符
    //转化成十六进制是防止特殊字符被篡改的问题
    private String hexEncode(byte[] buf) {
        return Hex.encodeHexString(buf, true)+"\n";
    }
    //将十六进制的数组转化为二进制的
    private byte[] hexDecode(String buf) throws DecoderException {
        return Hex.decodeHex(buf);
    }
}
