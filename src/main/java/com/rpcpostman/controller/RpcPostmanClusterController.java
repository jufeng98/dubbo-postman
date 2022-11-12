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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.rpcpostman.dto.WebApiRspDto;
import static com.rpcpostman.enums.RegisterCenterType.ZK;
import com.rpcpostman.service.AppFactory;
import com.rpcpostman.service.registry.RegisterFactory;
import com.rpcpostman.service.repository.redis.RedisKeys;
import com.rpcpostman.service.repository.redis.RedisRepository;
import com.rpcpostman.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author everythingbest
 * 目前提供动态添加和删除cluster的地址
 */
@Controller
@RequestMapping("/dubbo-postman/")
public class RpcPostmanClusterController {

    /**
     * 可执行修改其他的值,设置这个主要是防止随意修改注册地址
     */
    private final static String PASSWORD = "123456";
    @Autowired
    RedisRepository redisRepository;
    @Autowired
    private AppFactory appFactory;
    @Value("${dubbo.postman.env}")
    private String env;

    @RequestMapping(value = "configs", method = RequestMethod.GET)
    @ResponseBody
    public WebApiRspDto<List<Map<String, Object>>> configs() {

        Set<Object> sets = redisRepository.members(RedisKeys.CLUSTER_REDIS_KEY);

        return WebApiRspDto.success(setToMap(sets.stream().map(Object::toString).collect(Collectors.toSet())));
    }

    @RequestMapping(value = "all-zk", method = RequestMethod.GET)
    @ResponseBody
    public WebApiRspDto<List<Map<String, Object>>> allZk() {

        Set<String> zkAddrs = Sets.newHashSet();
        SpringUtil.getContext().getBeansOfType(RegisterFactory.class).values().stream()
                .map(RegisterFactory::getClusterSet)
                .forEach(zkAddrs::addAll);

        return WebApiRspDto.success(setToMap(zkAddrs));
    }

    private List<Map<String, Object>> setToMap(Set<String> zkAddrs) {
        return zkAddrs.stream()
                .map(addr -> {
                    Integer type = redisRepository.mapGet(RedisKeys.CLUSTER_REDIS_KEY_TYPE, addr);
                    if (type == null) {
                        type = ZK.type;
                    }
                    Map<String, Object> map = Maps.newHashMap();
                    map.put("addr", addr);
                    map.put("type", type);
                    return map;
                })
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "new/config", method = RequestMethod.GET)
    @ResponseBody
    public WebApiRspDto<String> queryDubbo(@RequestParam(name = "zk") String zk,
                                           @RequestParam(name = "password") String password,
                                           @RequestParam(name = "type") Integer type) {


        if (!password.equals(PASSWORD)) {
            return WebApiRspDto.error("密码错误");
        }

        if (zk.isEmpty()) {
            return WebApiRspDto.error("注册中心地址不能为空");
        }

        RegisterFactory registerFactory = AppFactory.getRegisterFactory(type);
        if (registerFactory.getClusterSet().contains(zk)) {
            return WebApiRspDto.error("注册中心地址已经存在");
        }


        registerFactory.addCluster(zk);

        registerFactory.get(zk);

        redisRepository.setAdd(RedisKeys.CLUSTER_REDIS_KEY, zk);
        redisRepository.mapPut(RedisKeys.CLUSTER_REDIS_KEY_TYPE, zk, type);

        return WebApiRspDto.success("保存成功");
    }

    @RequestMapping(value = "zk/del", method = RequestMethod.GET)
    @ResponseBody
    public WebApiRspDto<String> del(@RequestParam(name = "zk") String zk,
                                    @RequestParam(name = "password") String password) {

        if (password.equals(PASSWORD)) {
            if (zk.isEmpty()) {
                return WebApiRspDto.error("zk不能为空");
            }
            RegisterFactory registerFactory = appFactory.getRegisterFactory(zk);
            if (!registerFactory.getClusterSet().contains(zk)) {
                return WebApiRspDto.error("zk地址不存在");
            }

            registerFactory.getClusterSet().remove(zk);
            registerFactory.remove(zk);

            redisRepository.setRemove(RedisKeys.CLUSTER_REDIS_KEY, zk);
            redisRepository.removeMap(RedisKeys.CLUSTER_REDIS_KEY_TYPE, zk);
            return WebApiRspDto.success("删除成功");
        } else {
            return WebApiRspDto.error("密码错误");
        }
    }

    @RequestMapping(value = "env", method = RequestMethod.GET)
    @ResponseBody
    public WebApiRspDto<String> env() {
        return WebApiRspDto.success(env);
    }
}
