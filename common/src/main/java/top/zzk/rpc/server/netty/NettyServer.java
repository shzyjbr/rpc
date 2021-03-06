package top.zzk.rpc.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import top.zzk.rpc.server.AbstractRpcServer;
import top.zzk.rpc.codec.CommonDecoder;
import top.zzk.rpc.codec.CommonEncoder;
import top.zzk.rpc.registry.NacosServiceRegistry;
import top.zzk.rpc.serializer.Serializer;
import top.zzk.rpc.server.serviceprovider.ServiceProviderImpl;

import java.util.concurrent.TimeUnit;

/**
 * @author zzk
 * @date 2021/12/9
 * description 使用netty实现NIO方式通信的服务器
 */
@Slf4j
public class NettyServer extends AbstractRpcServer {


    /**
     * 配置文件中会配置序列化器（没配置则使用默认序列化器）,这里配置的话则会优先使用这里的配置
     * @param host
     * @param port
     */
    public NettyServer(String host, int port) {
        config();
        this.host = host;
        this.port = port;
        this.serviceProvider = new ServiceProviderImpl();
        this.serviceRegistry = new NacosServiceRegistry(registryHost, registryPort);
    }

    public NettyServer(String host, int port, int serializerCode) {
        this(host, port);
        this.serializer = Serializer.getByCode(serializerCode);
    }
    

    @Override
    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, 256)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            pipeline.addLast(new CommonEncoder(serializer));
                            pipeline.addLast(new CommonDecoder());
                            pipeline.addLast(new NettyServerHandler());
                        }
                    });
            ChannelFuture future = serverBootstrap.bind(host, port).sync();
//            new ShutdownHook(serviceRegistry).addHootForClearAllServices();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("启动服务器时发生错误:", e);
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
