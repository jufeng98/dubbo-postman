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

import com.rpcpostman.dto.WebApiRspDto;
import com.rpcpostman.service.Pair;
import com.rpcpostman.service.context.InvokeContext;
import com.rpcpostman.service.creation.entity.RequestParam;
import com.rpcpostman.service.creation.impl.EurekaCreator;
import com.rpcpostman.service.invocation.AbstractInvoker;
import com.rpcpostman.service.invocation.Invocation;
import com.rpcpostman.service.invocation.Invoker;
import com.rpcpostman.service.invocation.entity.DubboParamValue;
import com.rpcpostman.service.invocation.entity.PostmanDubboRequest;
import com.rpcpostman.util.BuildUtil;
import static com.rpcpostman.util.Constant.FEIGN_PARAM;
import com.rpcpostman.util.ThreadLocalUtil;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author everythingbest
 */
@Service
class FeignInvoker extends AbstractInvoker implements Invoker<Object, PostmanDubboRequest> {
    @Autowired
    private EurekaCreator creator;

    @SneakyThrows
    @Override
    public WebApiRspDto<Object> invoke(PostmanDubboRequest request, Invocation invocation) {
        String serviceKey = BuildUtil.buildServiceKey(request.getCluster(), request.getServiceName());
        GenericApplicationContext context = InvokeContext.getContext(serviceKey);
        if (context == null) {
            creator.initAndPutContext(request.getCluster(), request.getServiceName());
            context = InvokeContext.getContext(serviceKey);
        }
        Class<?> aClass = Objects.requireNonNull(context.getClassLoader()).loadClass(request.getInterfaceName());
        Object feignService = context.getBean(aClass);
        Class<?>[] parameterTypes = invocation.getParams().stream()
                .map(RequestParam::getTargetParaType).toArray(Class[]::new);
        Method method = aClass.getDeclaredMethod(invocation.getJavaMethodName(), parameterTypes);
        method.setAccessible(true);
        final DubboParamValue rpcParamValue = converter.convert(request, invocation);
        try {
            ThreadLocalUtil.set(FEIGN_PARAM, new Pair<>(request, rpcParamValue));
            Object res = method.invoke(feignService, rpcParamValue.getParamValues().toArray(new Object[0]));
            return WebApiRspDto.success(res);
        } finally {
            ThreadLocalUtil.reset(FEIGN_PARAM);
        }
    }

    @Override
    public WebApiRspDto<Object> invoke(Pair<PostmanDubboRequest, Invocation> pair) {
        return invoke(pair.getLeft(), pair.getRight());
    }

}
