package top.guoziyang.mydb.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


/**
* 编码Handler,将Package编码为字节流
* 作者：RioAngele
* 时间：2023.5.23
*/
public class EncoderHandler extends MessageToByteEncoder<Package> {
    Encoder encoder=new Encoder();
    @Override
    protected void encode(ChannelHandlerContext ctx, Package msg, ByteBuf out) throws Exception {
       out.writeBytes(encoder.encode(msg));
    }
}
