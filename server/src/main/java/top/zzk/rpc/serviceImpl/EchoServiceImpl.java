package top.zzk.rpc.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.zzk.rpc.server.annotation.Service;
import top.zzk.rpc.api.EchoService;
/*
    EchoService的具体实现
 */
@Service
public class EchoServiceImpl implements EchoService {
    private static final Logger logger = LoggerFactory.getLogger(EchoServiceImpl.class);
    
    @Override
    public String echo(String message) {
        logger.info("调用服务:#{}:echo,echo内容：{}", EchoServiceImpl.class.getCanonicalName(), message);
        return message;
    }
}
