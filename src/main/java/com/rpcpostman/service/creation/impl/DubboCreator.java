/*
 * MIT License
 *
 * Copyright (c) 2019 everythingbest
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rpcpostman.service.creation.impl;

import com.rpcpostman.service.GAV;
import com.rpcpostman.service.Pair;
import com.rpcpostman.service.creation.AbstractCreator;
import com.rpcpostman.service.creation.Creator;
import com.rpcpostman.service.creation.entity.DubboPostmanService;
import com.rpcpostman.service.creation.entity.InterfaceEntity;
import com.rpcpostman.service.creation.entity.PostmanService;
import com.rpcpostman.service.registry.entity.InterfaceMetaInfo;
import com.rpcpostman.service.registry.impl.DubboRegisterFactory;
import com.rpcpostman.service.repository.redis.RedisKeys;
import com.rpcpostman.util.BuildUtil;
import com.rpcpostman.util.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author everythingbest
 */
@Component
public class DubboCreator extends AbstractCreator implements Creator {

    @Autowired
    private DubboRegisterFactory dubboRegisterFactory;

    @Override
    public Pair<Boolean, String> create(String cluster, GAV gav, String serviceName) {

        Map<String, InterfaceMetaInfo> providers = dubboRegisterFactory.get(cluster)
                .getAllService().get(serviceName);

        DubboPostmanService dubboPostmanService = new DubboPostmanService();
        dubboPostmanService.setCluster(cluster);
        dubboPostmanService.setServiceName(serviceName);

        for (Map.Entry<String, InterfaceMetaInfo> entry : providers.entrySet()) {

            InterfaceEntity dubboInterfaceModel = new InterfaceEntity();

            String providerName = entry.getValue().getInterfaceName();
            String version = entry.getValue().getVersion();

            Set<String> serverIps = entry.getValue().getServerIps();
            Set<String> methodNames = entry.getValue().getMethodNames();

            dubboInterfaceModel.setKey(entry.getKey());
            dubboInterfaceModel.setInterfaceName(providerName);
            dubboInterfaceModel.setMethodNames(methodNames);
            dubboInterfaceModel.setServerIps(serverIps);
            dubboInterfaceModel.setVersion(version);
            dubboInterfaceModel.setGroup(entry.getValue().getGroup());

            dubboPostmanService.getInterfaceModels().add(dubboInterfaceModel);
        }

        dubboPostmanService.setGav(gav);
        dubboPostmanService.setGenerateTime(System.currentTimeMillis());

        return doCreateService(cluster, serviceName, dubboPostmanService);
    }

    @Override
    public Pair<Boolean, String> refresh(String cluster, String serviceName) {
        String serviceKey = BuildUtil.buildServiceKey(cluster, serviceName);
        Object serviceObj = redisRepository.mapGet(RedisKeys.RPC_MODEL_KEY, serviceKey);
        PostmanService postmanService = JSON.parseObject((String) serviceObj, DubboPostmanService.class);
        return doCreateService(cluster, serviceName, Objects.requireNonNull(postmanService));
    }

    private Pair<Boolean, String> doCreateService(String cluster,
                                                  String serviceName,
                                                  PostmanService postmanService) {

        GAV gav = postmanService.getGav();

        resolveMavenDependencies(serviceName, gav);

        Map<String, InterfaceMetaInfo> interfaceMetaInfoMap = dubboRegisterFactory.get(cluster)
                .getAllService().get(serviceName);

        if (logger.isDebugEnabled()) {
            logger.debug("应用名称:" + serviceName + "\n从ZK拉取的提供者:{}", JSON.objectToString(interfaceMetaInfoMap));
        }

        saveRedisAndLoad(postmanService);

        return new Pair<>(true, "成功");
    }

}
