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

import com.rpcpostman.service.context.InvokeContext;
import com.rpcpostman.service.creation.entity.PostmanService;
import com.rpcpostman.util.BuildUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author everythingbest
 * 提供一些公共的模板方法
 */
@Service
public abstract class AbstractController {

    Map<String, String> getAllSimpleClassName(String zk, String serviceName) {

        String modelKey = BuildUtil.buildServiceKey(zk, serviceName);
        Map<String, String> interfaceMap = new LinkedHashMap<>(10);
        PostmanService postmanService = InvokeContext.getService(modelKey);

        Objects.requireNonNull(postmanService).getInterfaceModelList().stream()
                .map(interfaceEntity -> {
                    String className = interfaceEntity.getInterfaceName();
                    String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
                    return Pair.of(simpleClassName, interfaceEntity.getKey());
                })
                .sorted(Comparator.comparing(Pair::getLeft))
                .forEach(pair -> interfaceMap.put(pair.getLeft(), pair.getRight()));
        return interfaceMap;
    }
}
