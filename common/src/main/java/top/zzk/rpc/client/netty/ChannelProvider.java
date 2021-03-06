package top.zzk.rpc.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import top.zzk.rpc.codec.CommonDecoder;
import top.zzk.rpc.codec.CommonEncoder;
import top.zzk.rpc.serializer.Serializer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author zzk
 * @date 2021/12/11
 * description 2022年5月27日-重构分析，把eventLoopGroup和bootstrap整合到一个ChannelProvider是为了定制serializer
 * 但是我觉得这样不太划算，每次都要重新把bootstrap的handle重新初始化一次，另外一点就是为了做个map把未完成的请求future存起来
 * 后续进行操作，但是这个可以用ChannelGroup来代替
 */
@Slf4j
public class ChannelProvider {
    private static EventLoopGroup workerGroup;
    private static final Bootstrap bootstrap = initializeBootstrap();
    private static final Map<String, Channel> channels = new ConcurrentHashMap<>();


    public static void shutdown() {
        workerGroup.shutdownGracefully();
    }

    public static Channel getChannel(InetSocketAddress inetSocketAddress, Serializer serializer) throws InterruptedException {
        String key = inetSocketAddress.toString() + serializer.getCode();
        if (channels.containsKey(key)) {
            Channel channel = channels.get(key);
            if (channel.isActive()) {
                return channel;
            } else {
                channels.remove(key);
            }
        }
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                socketChannel.pipeline()
                        .addLast(new CommonEncoder(serializer))
                        .addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS))
                        .addLast(new CommonDecoder())
                        .addLast(new NettyClientHandler());
            }
        });
        Channel channel = null;
        try {
            channel = connect(bootstrap, inetSocketAddress);
        } catch (ExecutionException e) {
            log.error("连接客户端时发生错误，", e);
            return null;
        }
        channels.put(key, channel);
        return channel;

    }


    private static Channel connect(Bootstrap bootstrap, InetSocketAddress socketAddress) throws InterruptedException, ExecutionException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(socketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("客户端连接成功");
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }

    private static Bootstrap initializeBootstrap() {
        workerGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                //连接超时时间，超过该时间则连接失败, 5s
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                //开启TCP底层心跳机制
                .option(ChannelOption.SO_KEEPALIVE, true)
                //TCP默认开启Nagle算法，关闭它。Nagle算法会尽可能地发送大数据块，从而导致小块数据在缓冲区停留
                .option(ChannelOption.TCP_NODELAY, true);
        return bootstrap;
    }
}
