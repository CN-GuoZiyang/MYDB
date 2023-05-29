package top.guoziyang.mydb.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import top.guoziyang.mydb.transport.Package;


/**
* 改用Netty客户端Handler
* 作者：RioAngele
* 时间：2023.5.23
*/
public class NettyClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyClient.resultFuture.complete((Package) msg);
    }
}
