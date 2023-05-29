package top.guoziyang.mydb.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import top.guoziyang.mydb.transport.DecoderHandler;
import top.guoziyang.mydb.transport.EncoderHandler;
import top.guoziyang.mydb.transport.Package;


import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
* Netty客户端
* 作者：RioAngele
* 时间：2023.5.23
*/
public final class NettyClient  {

    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    public Channel channel;
    public static CompletableFuture<Package> resultFuture;

    public NettyClient() throws InterruptedException {
        CompletableFuture<byte[]> resultFuture= new CompletableFuture<>();
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new EncoderHandler())
                                .addLast(new DecoderHandler())
                                .addLast(new NettyClientHandler());
                    }
                });
        channel=bootstrap.connect("127.0.0.1",7777).sync().channel();
    }



    public byte[] execute(byte[] sh) throws Exception {
        resultFuture=new CompletableFuture<Package>();
        Package pkg=new Package(sh,null);
        if (channel.isActive()) {
            channel.writeAndFlush(pkg).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    System.out.println("success to send");
                } else {
                    System.out.println("fail to send");
                    future.channel().close();
                }
            });
        } else {
            throw new IllegalStateException();
        }
        Package resPkg=null;
        while(!resultFuture.isDone()){


        }
        resPkg=resultFuture.get();
        if(resPkg.getErr() != null) {
            throw resPkg.getErr();
        }
        return resPkg.getData();
    }

    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}

