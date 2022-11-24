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

import com.google.common.collect.Lists;
import com.rpcpostman.service.GAV;
import com.rpcpostman.service.Pair;
import com.rpcpostman.service.context.InvokeContext;
import com.rpcpostman.service.creation.AbstractCreator;
import com.rpcpostman.service.creation.Creator;
import com.rpcpostman.service.creation.entity.DubboPostmanService;
import com.rpcpostman.service.creation.entity.InterfaceEntity;
import com.rpcpostman.service.creation.entity.PostmanService;
import com.rpcpostman.service.load.classloader.ApiJarClassLoader;
import com.rpcpostman.service.load.impl.JarLocalFileLoader;
import com.rpcpostman.service.registry.impl.EurekaRegister;
import com.rpcpostman.service.repository.redis.RedisKeys;
import com.rpcpostman.util.BuildUtil;
import com.rpcpostman.util.Constant;
import com.rpcpostman.util.JSON;
import lombok.SneakyThrows;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignServiceRegistrar;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * @author everythingbest
 */
@Component
public
class EurekaCreator extends AbstractCreator implements Creator {

    @SneakyThrows
    @Override
    public Pair<Boolean, String> create(String cluster, GAV gav, String serviceName) {
        resolveMavenDependencies(serviceName, gav);
        DubboPostmanService postmanService = new DubboPostmanService();
        postmanService.setCluster(cluster);
        postmanService.setServiceName(serviceName);
        postmanService.setGav(gav);
        postmanService.setGenerateTime(System.currentTimeMillis());

        List<Class<?>> feignClientClasses = loadAllFeignClientClass(serviceName, gav);
        List<InterfaceEntity> list = getAllFeignClientMethods(feignClientClasses);
        postmanService.getInterfaceModels().addAll(list);

        saveRedisAndLoad(postmanService);

        String serviceKey = BuildUtil.buildServiceKey(postmanService.getCluster(), postmanService.getServiceName());
        GenericApplicationContext context = FeignServiceRegistrar.register(gav, JarLocalFileLoader.getAllClassLoader().get(serviceKey));
        InvokeContext.putContext(serviceKey, context);
        return new Pair<>(true, "成功");
    }

    @Override
    public Pair<Boolean, String> refresh(String cluster, String serviceName) {
        EurekaRegister.clearCache();
        String serviceKey = BuildUtil.buildServiceKey(cluster, serviceName);
        Object serviceObj = redisRepository.mapGet(RedisKeys.RPC_MODEL_KEY, serviceKey);
        PostmanService postmanService = JSON.parseObject((String) serviceObj, DubboPostmanService.class);
        create(cluster, Objects.requireNonNull(postmanService).getGav(), serviceName);
        return new Pair<>(true, "成功");
    }

    public GenericApplicationContext initAndPutContext(String cluster, String serviceName) {
        String serviceKey = BuildUtil.buildServiceKey(cluster, serviceName);
        PostmanService service = InvokeContext.getService(serviceKey);
        GenericApplicationContext context = FeignServiceRegistrar.register(service.getGav(),
                JarLocalFileLoader.getAllClassLoader().get(serviceKey));
        InvokeContext.putContext(serviceKey, context);
        return context;
    }

    @SneakyThrows
    private List<Class<?>> loadAllFeignClientClass(String serviceName, GAV gav) {
        ApiJarClassLoader apiJarClassLoader = JarLocalFileLoader.initClassLoader(serviceName, gav.getVersion());
        String apiFilePath = JarLocalFileLoader.getApiFilePath(serviceName, gav);
        JarFile jarFile = new JarFile(apiFilePath);
        Enumeration<JarEntry> enumeration = jarFile.entries();
        List<Class<?>> list = Lists.newArrayList();
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement();
            String name = jarEntry.getName();
            if (!name.endsWith(".class")) {
                continue;
            }
            name = name.replace(".class", "").replace("/", ".");
            Class<?> aClass = apiJarClassLoader.loadClassWithResolve(name);
            if (!aClass.isInterface()) {
                continue;
            }
            FeignClient feignClient = aClass.getAnnotation(FeignClient.class);
            if (feignClient == null) {
                continue;
            }
            list.add(aClass);
        }
        return list;
    }

    public List<InterfaceEntity> getAllFeignClientMethods(List<Class<?>> classes) {
        return classes.stream()
                .map(aClass -> {
                    InterfaceEntity interfaceModel = new InterfaceEntity();
                    String providerName = aClass.getName();
                    Set<String> methodNames = Arrays.stream(aClass.getDeclaredMethods())
                            .map(Method::getName)
                            .collect(Collectors.toSet());
                    interfaceModel.setInterfaceName(providerName);
                    interfaceModel.setMethodNames(methodNames);
                    interfaceModel.setServerIps(Collections.emptySet());
                    interfaceModel.setVersion(Constant.DEFAULT_VERSION);
                    interfaceModel.setGroup(Constant.GROUP_DEFAULT);
                    interfaceModel.setKey(BuildUtil.buildInterfaceKey(interfaceModel.getGroup(), providerName, interfaceModel.getVersion()));
                    return interfaceModel;
                })
                .collect(Collectors.toList());
    }
}
