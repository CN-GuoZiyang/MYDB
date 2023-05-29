package top.guoziyang.mydb.backend.server;



import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import top.guoziyang.mydb.backend.tbm.TableManager;
import top.guoziyang.mydb.transport.DecoderHandler;
import top.guoziyang.mydb.transport.EncoderHandler;


/**
 * netty服务端
 * 作者：RioAngele
 * 时间：2023.5.23
 */

public class NettyServer {

     int PORT ;
    TableManager tbm;
    public NettyServer(int PORT, TableManager tbm){
        this.PORT=PORT;
        this.tbm=tbm;
    }



    public void start() {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new EncoderHandler())
                                    .addLast(new DecoderHandler())
                                    .addLast(new NettyServerHandler(tbm));
                        }
                    });

            ChannelFuture f = b.bind(PORT).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("fail to start!!!");
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }


}

