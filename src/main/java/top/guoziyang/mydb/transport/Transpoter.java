package top.guoziyang.mydb.transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.primitives.Bytes;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class Transpoter {
    private Socket socket;
    private BufferedInputStream reader;
    private BufferedOutputStream writer;

    public Transpoter(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedInputStream(socket.getInputStream());
        this.writer = new BufferedOutputStream(socket.getOutputStream());
    }

    public void send(byte[] data) throws Exception {
        byte[] raw = hexEncode(data);
        writer.write(raw);
        writer.flush();
    }

    public byte[] receive() throws Exception {
        List<Byte> bytes = new ArrayList<>();
        while(true) {
            byte b = (byte)reader.read();
            bytes.add(b);
            if(b == '\n') {
                break;
            }
        }
        return hexDecode(Bytes.toArray(bytes));
    }

    public void close() throws IOException {
        socket.close();
    }

    private byte[] hexEncode(byte[] buf) {
        return (Hex.encodeHexString(buf, true)+"\n").getBytes();
    }

    private byte[] hexDecode(byte[] buf) throws DecoderException {
        return Hex.decodeHex(new String(Arrays.copyOf(buf, buf.length-1)));
    }
}
