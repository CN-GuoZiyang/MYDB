package top.guoziyang.mydb.backend.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import top.guoziyang.mydb.backend.tbm.TableManager;
import top.guoziyang.mydb.transport.Encoder;
import top.guoziyang.mydb.transport.Package;
import top.guoziyang.mydb.transport.Packager;


/**
 * netty服务端Handler
 * 作者：RioAngele
 * 时间：2023.5.23
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {
    private TableManager tbm;

    public NettyServerHandler(TableManager tbm) {
        this.tbm = tbm;
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Package pck=(Package) msg;
        Executor exe = new Executor(tbm);
        byte[] result = null;
        Exception e = null;
        try {
            result = exe.execute(pck.getData());
        } catch (Exception e1) {
            e = e1;
            e.printStackTrace();
            exe.close();
        }
        Package pkg=new Package(result,e);
        ctx.writeAndFlush(pkg);
    }
}
