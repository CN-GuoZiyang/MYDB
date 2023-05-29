package top.guoziyang.mydb.transport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.List;


/**
* 解码Handler,将字节流解码为Package
* 作者：RioAngele
* 时间：2023.5.23
*/
public class DecoderHandler extends ByteToMessageDecoder{
    Encoder encoder=new Encoder();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes()>0){
        byte[] by=new byte[in.readableBytes()];
        in.readBytes(by);
        out.add(encoder.decode(by));}
    }
}
