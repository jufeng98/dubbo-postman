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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpcpostman.model.*;
import com.rpcpostman.model.erd.ErdOnlineModel;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author yu
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ErdController {
    public static final String prefix = "mh-wash:erd:";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @RequestMapping(value = "/erd/oauth/token", method = {RequestMethod.GET, RequestMethod.POST})
    public String token() {
        return stringRedisTemplate.opsForValue().get(prefix + "token");
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/statistic", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<StatisticVo> statistic() {
        String jsonDataStr = stringRedisTemplate.opsForValue().get(prefix + "statistic");
        if (jsonDataStr == null) {
            jsonDataStr = "{\"yesterday\":7,\"total\":1334,\"month\":48,\"today\":5}";
            stringRedisTemplate.opsForValue().set(prefix + "statistic", jsonDataStr);
        }
        StatisticVo statisticVo = objectMapper.readValue(jsonDataStr, StatisticVo.class);
        return ResultVo.success(statisticVo);
    }

    @SneakyThrows
    @RequestMapping(value = {"/ncnb/project/page", "/ncnb/project/recent"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<PageVo> page() {
        String jsonDataStr = stringRedisTemplate.opsForValue().get(prefix + "page");
        if (jsonDataStr == null) {
            jsonDataStr = "{\"records\":[],\"total\":0,\"size\":100,\"current\":1,\"orders\":[],\"searchCount\":true,\"pages\":1}";
            stringRedisTemplate.opsForValue().set(prefix + "page", jsonDataStr);
        }
        PageVo pageVo = objectMapper.readValue(jsonDataStr, PageVo.class);
        return ResultVo.success(pageVo);
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/info/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<ErdOnlineModel> info(@PathVariable String id) {
        String jsonDataStr = stringRedisTemplate.opsForValue().get(prefix + id);
        ErdOnlineModel erdOnlineModel = objectMapper.readValue(jsonDataStr, ErdOnlineModel.class);
        return ResultVo.success(erdOnlineModel);
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/add", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<String> add(@RequestBody JSONObject jsonObjectReq) {
        String id = UUID.randomUUID().toString().replace("-", "");
        RecordsVo recordsVo = new RecordsVo();
        recordsVo.setId(id);
        recordsVo.setProjectName(jsonObjectReq.getString("projectName"));
        recordsVo.setDescription(jsonObjectReq.getString("description"));
        recordsVo.setTags(jsonObjectReq.getString("tags"));
        recordsVo.setType("1");

        String jsonDataStr = stringRedisTemplate.opsForValue().get(prefix + "page");
        PageVo pageVo = objectMapper.readValue(jsonDataStr, PageVo.class);
        List<RecordsVo> records = Objects.requireNonNull(pageVo).getRecords();
        records.add(recordsVo);
        pageVo.setTotal(records.size());
        stringRedisTemplate.opsForValue().set(prefix + "page", objectMapper.writeValueAsString(pageVo));

        ErdOnlineModel erdOnlineModel = objectMapper.readValue(jsonObjectReq.toJSONString(), ErdOnlineModel.class);
        erdOnlineModel.setId(id);
        erdOnlineModel.setProjectName(recordsVo.getProjectName());
        erdOnlineModel.setType("1");
        stringRedisTemplate.opsForValue().set(prefix + id, objectMapper.writeValueAsString(erdOnlineModel));

        stringRedisTemplate.opsForValue().set(prefix + "load:" + id, "[]");

        return ResultVo.success("新建项目成功");
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/update", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<String> update(@RequestBody RecordsVo recordsVoReq) {
        String jsonDataStr = stringRedisTemplate.opsForValue().get(prefix + "page");
        PageVo pageVo = objectMapper.readValue(jsonDataStr, PageVo.class);
        List<RecordsVo> records = Objects.requireNonNull(pageVo).getRecords();
        for (RecordsVo record : records) {
            if (record.getId().equals(recordsVoReq.getId())) {
                record.setProjectName(recordsVoReq.getProjectName());
                record.setDescription(recordsVoReq.getDescription());
                record.setTags(recordsVoReq.getTags());
                break;
            }
        }
        stringRedisTemplate.opsForValue().set(prefix + "page", objectMapper.writeValueAsString(pageVo));

        jsonDataStr = stringRedisTemplate.opsForValue().get(prefix + "" + recordsVoReq.getId());
        ErdOnlineModel erdOnlineModel = objectMapper.readValue(jsonDataStr, ErdOnlineModel.class);
        Objects.requireNonNull(erdOnlineModel).setProjectName(recordsVoReq.getProjectName());
        stringRedisTemplate.opsForValue().set(prefix + recordsVoReq.getId(), objectMapper.writeValueAsString(erdOnlineModel));

        return ResultVo.success("更新项目成功");
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/save", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<Boolean> save(@RequestBody ErdOnlineModel erdOnlineModelReq) {
        String id = erdOnlineModelReq.getId();
        stringRedisTemplate.opsForValue().set(prefix + id, objectMapper.writeValueAsString(erdOnlineModelReq));
        return ResultVo.success(true);
    }


    @RequestMapping(value = "/ncnb/hisProject/load", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<JSONArray> load(@RequestBody JSONObject jsonObjectReq) {
        String projectId = jsonObjectReq.getString("projectId");
        String jsonStr = stringRedisTemplate.opsForValue().get(prefix + "load:" + projectId);
        JSONArray jsonArrayData = JSONObject.parseArray(jsonStr);
        return ResultVo.success(jsonArrayData);
    }
}
