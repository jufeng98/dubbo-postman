package com.rpcpostman.service.creation;

import com.rpcpostman.service.GAV;
import com.rpcpostman.service.context.InvokeContext;
import com.rpcpostman.service.creation.entity.PostmanService;
import com.rpcpostman.service.load.impl.JarLocalFileLoader;
import com.rpcpostman.service.maven.Maven;
import com.rpcpostman.service.repository.redis.RedisKeys;
import com.rpcpostman.service.repository.redis.RedisRepository;
import com.rpcpostman.util.BuildUtil;
import com.rpcpostman.util.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author yudong
 * @date 2022/11/13
 */
public abstract class AbstractCreator {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    protected RedisRepository redisRepository;
    @Autowired
    protected Maven maven;

    protected void resolveMavenDependencies(String serviceName, GAV gav) {
        logger.info("开始创建服务...");
        logger.info("如果系统是第一次构建服务则需要下载各种maven plugin,耗时比较长");
        maven.dependency(serviceName, gav);
    }

    protected void saveRedisAndLoad(PostmanService postmanService) {
        String serviceString = JSON.objectToString(postmanService);
        String serviceKey = BuildUtil.buildServiceKey(postmanService.getCluster(), postmanService.getServiceName());
        redisRepository.mapPut(RedisKeys.RPC_MODEL_KEY, serviceKey, serviceString);
        redisRepository.setAdd(postmanService.getCluster(), postmanService.getServiceName());

        JarLocalFileLoader.loadRuntimeInfo(postmanService);
        InvokeContext.putService(serviceKey, postmanService);
    }
}
