package com.rpcpostman.service;

import com.rpcpostman.enums.RegisterCenterType;
import com.rpcpostman.service.creation.Creator;
import com.rpcpostman.service.creation.impl.DubboCreator;
import com.rpcpostman.service.creation.impl.EurekaCreator;
import com.rpcpostman.service.invocation.Invoker;
import com.rpcpostman.service.invocation.entity.PostmanDubboRequest;
import com.rpcpostman.service.registry.RegisterFactory;
import com.rpcpostman.service.registry.impl.DubboRegisterFactory;
import com.rpcpostman.service.registry.impl.EurekaRegisterFactory;
import com.rpcpostman.service.repository.redis.RedisKeys;
import com.rpcpostman.service.repository.redis.RedisRepository;
import com.rpcpostman.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author yudong
 * @date 2022/11/12
 */
@Component
public class AppFactory {
    @Autowired
    private RedisRepository redisRepository;
    @Autowired
    private Invoker<Object, PostmanDubboRequest> dubboInvoker;
    @Autowired
    @Qualifier("feignInvoker")
    private Invoker<Object, PostmanDubboRequest> feignInvoker;


    public RegisterFactory getRegisterFactory(String zk) {
        Integer type = redisRepository.mapGet(RedisKeys.CLUSTER_REDIS_KEY_TYPE, zk);
        return getRegisterFactory(type);
    }

    public static RegisterFactory getRegisterFactory(Integer type) {
        RegisterCenterType registrationCenterType = RegisterCenterType.getByType(type);
        if (registrationCenterType == RegisterCenterType.ZK) {
            return SpringUtil.getContext().getBean(DubboRegisterFactory.class);
        } else if (registrationCenterType == RegisterCenterType.EUREKA) {
            return SpringUtil.getContext().getBean(EurekaRegisterFactory.class);
        }
        throw new RuntimeException("wrong type:" + type);
    }


    public Invoker<Object, PostmanDubboRequest> getInvoker(String cluster) {
        Integer type = redisRepository.mapGet(RedisKeys.CLUSTER_REDIS_KEY_TYPE, cluster);
        RegisterCenterType centerType = RegisterCenterType.getByType(type);
        if (centerType == RegisterCenterType.ZK) {
            return dubboInvoker;
        } else if (centerType == RegisterCenterType.EUREKA) {
            return feignInvoker;
        } else {
            throw new RuntimeException("wrong type:" + type);
        }
    }

    public Creator getCreator(String zk) {
        Integer type = redisRepository.mapGet(RedisKeys.CLUSTER_REDIS_KEY_TYPE, zk);
        RegisterCenterType registrationCenterType = RegisterCenterType.getByType(type);
        if (registrationCenterType == RegisterCenterType.ZK) {
            return SpringUtil.getContext().getBean(DubboCreator.class);
        } else if (registrationCenterType == RegisterCenterType.EUREKA) {
            return SpringUtil.getContext().getBean(EurekaCreator.class);
        }
        throw new RuntimeException("wrong type:" + type);
    }
}
