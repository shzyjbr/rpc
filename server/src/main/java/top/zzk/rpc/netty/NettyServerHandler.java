package top.zzk.rpc.netty;

import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import top.zzk.rpc.RequestHandler;
import top.zzk.rpc.common.entity.RpcRequest;
import top.zzk.rpc.common.entity.RpcResponse;
import top.zzk.rpc.common.factory.ThreadPoolFactory;

import java.util.concurrent.ExecutorService;

/**
 * @author zzk
 * @date 2021/12/9
 * description Netty服务器入站处理，在这里可以对入站数据进行处理
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    
    private static final ExecutorService threadPool = ThreadPoolFactory.createDefaultThreadPool("netty-server-handler");
    

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        threadPool.execute( () -> {
            try {
                log.info("服务器接收到请求:{}", msg);
                Object result = RequestHandler.handle(msg);
                ChannelFuture future = ctx.writeAndFlush(RpcResponse.success(result,msg.getRequestId()));
                future.addListener(ChannelFutureListener.CLOSE);
            } finally {
                ReferenceCountUtil.release(msg);
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理过程调用时发生错误:{}", cause.getMessage());
        ctx.close();
    }
}
