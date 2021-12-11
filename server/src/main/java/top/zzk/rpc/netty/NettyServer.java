package top.zzk.rpc.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;
import top.zzk.rpc.RpcServer;
import top.zzk.rpc.common.codec.CommonDecoder;
import top.zzk.rpc.common.codec.CommonEncoder;
import top.zzk.rpc.common.enumeration.RpcError;
import top.zzk.rpc.common.exception.RpcException;
import top.zzk.rpc.common.registry.ServiceRegistry;
import top.zzk.rpc.common.serializer.HessianSerializer;
import top.zzk.rpc.common.serializer.JsonSerializer;
import top.zzk.rpc.common.serializer.KryoSerializer;
import top.zzk.rpc.common.serializer.Serializer;

/**
 * @author zzk
 * @date 2021/12/9
 * description 使用netty实现NIO方式通信的服务器
 */
@Slf4j
public class NettyServer implements RpcServer {
    
    private ServiceRegistry registry;
    private Serializer serializer;

    public NettyServer(ServiceRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void start(int port) {
        if (serializer == null) {
            log.error("序列化器未初始化");
            throw new RpcException(RpcError.SERIALIZER_UNDEFINED);
        }
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, 256)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new CommonEncoder(serializer));
                            pipeline.addLast(new CommonDecoder());
                            pipeline.addLast(new NettyServerHandler(registry));
                        }
                    });
            ChannelFuture future = serverBootstrap.bind(port).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("启动服务器时发生错误:", e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }

    }

    @Override
    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }
}