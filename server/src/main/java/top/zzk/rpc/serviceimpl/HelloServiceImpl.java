package top.zzk.rpc.serviceimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.zzk.rpc.api.HelloObject;
import top.zzk.rpc.api.HelloService;

/**
 * @author zzk
 * @date 2021/11/28
 * description
 */
public class HelloServiceImpl implements HelloService {

    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);
    
    @Override
    public String hello(HelloObject object) {
        return "reply for \"" + object.getMessage() + "\":welcome! ";
    }
}
