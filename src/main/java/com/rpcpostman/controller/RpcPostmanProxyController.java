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

package com.rpcpostman.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.rpcpostman.dto.WebApiRspDto;
import com.rpcpostman.service.AppFactory;
import com.rpcpostman.service.Pair;
import com.rpcpostman.service.context.InvokeContext;
import com.rpcpostman.service.invocation.Invocation;
import com.rpcpostman.service.invocation.Invoker;
import com.rpcpostman.service.invocation.entity.PostmanDubboRequest;
import com.rpcpostman.util.JSON;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author everythingbest
 * 访问dubbo的对外接口
 */
@Controller
public class RpcPostmanProxyController {

    private static final Logger logger = LoggerFactory.getLogger(RpcPostmanProxyController.class);
    @Autowired
    private AppFactory appFactory;

    @SneakyThrows
    @RequestMapping(value = "/dubbo", method = RequestMethod.POST)
    @ResponseBody
    public WebApiRspDto<Object> dubbo(@RequestBody JsonNode jsonNode) {
        String cluster = jsonNode.get("cluster").asText();
        String serviceName = jsonNode.get("serviceName").asText();
        String interfaceKey = jsonNode.get("interfaceKey").asText();
        String methodName = jsonNode.get("methodName").asText();
        String dubboParam = jsonNode.get("dubboParam").asText();
        String dubboIp = jsonNode.get("dubboIp").asText();
        Invoker<Object, PostmanDubboRequest> invoker = appFactory.getInvoker(cluster);
        Pair<PostmanDubboRequest, Invocation> pair = InvokeContext.buildInvocation(cluster, serviceName, interfaceKey,
                methodName, dubboParam, dubboIp);
        PostmanDubboRequest request = pair.getLeft();
        Invocation invocation = pair.getRight();

        if (logger.isDebugEnabled()) {
            logger.debug("接收RPC-POSTMAN请求:" + JSON.objectToString(request));
        }

        return invoker.invoke(request, invocation);
    }
}
