package top.zzk.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.zzk.rpc.common.entity.RpcRequest;
import top.zzk.rpc.common.entity.RpcResponse;
import top.zzk.rpc.common.enumeration.RpcResponseCode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author zzk
 * @date 2021/11/28
 * description 服务端请求处理器，对请求进行具体服务调用
 */
public class RequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    public static Object handle(RpcRequest rpcRequest, Object service) {
        Object result = null;
        try {
            result = RequestHandler.invokeMethod(rpcRequest, service);
            logger.info("服务:{} 成功调用{} 方法", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (InvocationTargetException | IllegalAccessException e) {
            logger.info("调用或发送时有错误发生：", e);
        }
        return result;
    }

    private static Object invokeMethod(RpcRequest request, Object service) throws InvocationTargetException, IllegalAccessException {
        Method method;
        try {
            method = service.getClass().getMethod(request.getMethodName(), request.getParamTypes());
        } catch (NoSuchMethodException e) {
            return RpcResponse.fail(RpcResponseCode.NOT_FOUND_METHOD, request.getRequestId());
        }
        return method.invoke(service, request.getParams());
    }
}
