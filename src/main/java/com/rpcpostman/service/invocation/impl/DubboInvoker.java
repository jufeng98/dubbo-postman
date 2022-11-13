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

package com.rpcpostman.service.invocation.impl;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.rpcpostman.dto.WebApiRspDto;
import com.rpcpostman.service.Pair;
import com.rpcpostman.service.invocation.AbstractInvoker;
import com.rpcpostman.service.invocation.Invocation;
import com.rpcpostman.service.invocation.Invoker;
import com.rpcpostman.service.invocation.entity.DubboParamValue;
import com.rpcpostman.service.invocation.entity.PostmanDubboRequest;
import com.rpcpostman.util.Constant;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author everythingbest
 */
@Primary
@Service
class DubboInvoker extends AbstractInvoker implements Invoker<Object, PostmanDubboRequest> {

    private final ApplicationConfig application = new ApplicationConfig(Constant.APP_NAME);

    private final Map<String, ReferenceConfig<GenericService>> cachedReference = new WeakHashMap<>();


    @SneakyThrows
    @Override
    public WebApiRspDto<Object> invoke(PostmanDubboRequest request, Invocation invocation) {

        final DubboParamValue rpcParamValue = converter.convert(request, invocation);

        GenericService service = getOrCreateService(request, rpcParamValue);
        long start = System.currentTimeMillis();

        Object obj = service.$invoke(invocation.getJavaMethodName(), rpcParamValue.getParamTypeNames().toArray(new String[]{}), rpcParamValue.getParamValues().toArray());

        long end = System.currentTimeMillis();
        long elapse = end - start;
        logger.info("请求dubbo耗时:" + elapse);

        return WebApiRspDto.success(obj, elapse);

    }

    @Override
    public WebApiRspDto<Object> invoke(Pair<PostmanDubboRequest, Invocation> pair) {
        return invoke(pair.getLeft(), pair.getRight());
    }

    private GenericService getOrCreateService(PostmanDubboRequest request, DubboParamValue rpcParamValue) throws Exception {

        String serviceName = request.getServiceName();
        String group = request.getGroup();
        String interfaceName = request.getInterfaceName();
        String referenceKey = serviceName + "-" + group + "-" + interfaceName;
        String cacheKey;

        if (rpcParamValue.isUseDubbo()) {
            cacheKey = rpcParamValue.getDubboUrl() + "-" + referenceKey;
        } else {
            cacheKey = rpcParamValue.getRegistry() + "-" + referenceKey;
        }

        ReferenceConfig<GenericService> reference = cachedReference.get(cacheKey);

        if (reference == null) {
            synchronized (DubboInvoker.class) {
                reference = cachedReference.get(cacheKey);
                if (reference == null) {
                    GenericService service;
                    try {
                        reference = createReference(request, rpcParamValue);
                        service = reference.get();
                    } catch (Exception e) {
                        //尝试不设置版本号
                        request.setVersion(null);
                        reference = createReference(request, rpcParamValue);
                        service = reference.get();
                    }
                    //如果创建成功了就添加，否则不添加
                    if (service != null) {
                        cachedReference.put(cacheKey, reference);
                    }
                }
            }
        }

        GenericService service = reference.get();
        //如果创建失败了,比如provider重启了,需要重新创建
        if (service == null) {
            cachedReference.remove(cacheKey);
            throw new Exception("ReferenceConfig创建GenericService失败,检查provider是否启动");
        }
        return service;
    }

    ReferenceConfig<GenericService> createReference(PostmanDubboRequest request, DubboParamValue rpcParamValue) {

        ReferenceConfig<GenericService> newReference = new ReferenceConfig<>();

        newReference.setTimeout(10000);
        newReference.setApplication(application);
        newReference.setInterface(request.getInterfaceName());

        String group = request.getGroup();

        //default是我加的,dubbo默认是没有的
        if (StringUtils.isNotBlank(group) && !group.equals("default")) {
            newReference.setGroup(group);
        }

        if (rpcParamValue.isUseDubbo()) {
            //直连
            newReference.setUrl(rpcParamValue.getDubboUrl());
            logger.info("直连dubbo地址:" + rpcParamValue.getDubboUrl());
        } else {
            //通过zk访问
            newReference.setRegistry(new RegistryConfig(rpcParamValue.getRegistry()));
        }

        newReference.setVersion(request.getVersion());
        newReference.setGeneric(true);
        //hard code
        newReference.setRetries(1);

        return newReference;
    }
}
