package top.guoziyang.mydb.transport;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.assertTrue;


/**
* 解码编码Handler的单元测试
* 作者：RioAngele
* 时间：2023.5.29
*/
public class CodecTest {
    @Test
    public void decodeTest(){
        ByteBuf buf= Unpooled.buffer();
        buf.writeInt(1);
        buf.writeBytes("create table".getBytes());
        ByteBuf input=buf.duplicate();
        EmbeddedChannel channel=new EmbeddedChannel(new DecoderHandler());
        channel.writeInbound(input);
        assertTrue(channel.finish());

        Package a=channel.readInbound();
        System.out.println(new String(a.getData()));
    }

    @Test
    public void encodeTest(){
        Package pck=new Package("create table".getBytes(),null);

        EmbeddedChannel channel=new EmbeddedChannel(new EncoderHandler());

        channel.writeOutbound(pck);
        assertTrue(channel.finish());

        ByteBuf buf=channel.readOutbound();
        byte[] by=new byte[buf.readableBytes()];
        buf.readBytes(by);
        System.out.println(new String(by));
    }
}
