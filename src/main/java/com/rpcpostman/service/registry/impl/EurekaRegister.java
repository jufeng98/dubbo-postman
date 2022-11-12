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

package com.rpcpostman.service.registry.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rpcpostman.service.registry.Register;
import com.rpcpostman.service.registry.entity.InterfaceMetaInfo;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author yudong
 */
public class EurekaRegister implements Register {

    private static final Map<String, List<String>> INSTANCES_CACHE = new ConcurrentHashMap<>();
    private final String cluster;
    private final RestTemplate restTemplate;

    public EurekaRegister(String cluster, RestTemplate restTemplate) {
        this.cluster = cluster;
        this.restTemplate = restTemplate;
    }

    public static void clearCache() {
        INSTANCES_CACHE.clear();
    }

    @Override
    public void pullData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Map<String, InterfaceMetaInfo>> getAllService() {
        Map<String, Map<String, InterfaceMetaInfo>> map = Maps.newHashMap();
        JsonNode jsonNode = restTemplate.getForObject("http://" + cluster + "/eureka/apps/", JsonNode.class);
        ArrayNode arrayNode = (ArrayNode) jsonNode.get("applications").get("application");
        for (JsonNode node : arrayNode) {
            map.put(node.get("name").asText(), Collections.emptyMap());
        }
        return map;
    }

    @Override
    public List<String> getServiceInstances(String serviceName) {
        List<String> instances = INSTANCES_CACHE.get(serviceName);
        if (instances != null) {
            return instances;
        }
        instances = Lists.newArrayList();
        JsonNode jsonNode = restTemplate.getForObject("http://" + cluster + "/eureka/apps/" + serviceName, JsonNode.class);
        ArrayNode arrayNode = (ArrayNode) jsonNode.get("application").get("instance");
        for (JsonNode node : arrayNode) {
            instances.add(node.get("ipAddr").asText() + ":" + node.get("port").get("$").asText());
        }
        INSTANCES_CACHE.put(serviceName, instances);
        return instances;
    }
}
