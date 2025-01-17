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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.*;
import com.rpcpostman.model.*;
import com.rpcpostman.model.erd.*;
import com.rpcpostman.util.DbUtils;
import com.rpcpostman.util.ErdUtils;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author yu
 */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class ErdController {
    public static final String PREFIX = "mh-wash:erd:";
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    @RequestMapping(value = "/erd/oauth/token", method = {RequestMethod.GET, RequestMethod.POST})
    public String token() {
        return stringRedisTemplate.opsForValue().get(PREFIX + "token");
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/statistic", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<StatisticVo> statistic() {
        String jsonDataStr = stringRedisTemplate.opsForValue().get(PREFIX + "statistic");
        if (jsonDataStr == null) {
            jsonDataStr = "{\"yesterday\":7,\"total\":1334,\"month\":48,\"today\":5}";
            stringRedisTemplate.opsForValue().set(PREFIX + "statistic", jsonDataStr);
        }
        StatisticVo statisticVo = objectMapper.readValue(jsonDataStr, StatisticVo.class);
        return ResultVo.success(statisticVo);
    }

    @SneakyThrows
    @RequestMapping(value = {"/ncnb/project/page", "/ncnb/project/recent"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<PageVo> page() {
        String jsonDataStr = stringRedisTemplate.opsForValue().get(PREFIX + "page");
        if (jsonDataStr == null) {
            jsonDataStr = "{\"records\":[],\"total\":0,\"size\":100,\"current\":1,\"orders\":[],\"searchCount\":true,\"pages\":1}";
            stringRedisTemplate.opsForValue().set(PREFIX + "page", jsonDataStr);
        }
        PageVo pageVo = objectMapper.readValue(jsonDataStr, PageVo.class);
        return ResultVo.success(pageVo);
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/info/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<ErdOnlineModel> info(@PathVariable String id) {
        String jsonDataStr = stringRedisTemplate.opsForValue().get(PREFIX + id);
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

        String jsonDataStr = stringRedisTemplate.opsForValue().get(PREFIX + "page");
        PageVo pageVo = objectMapper.readValue(jsonDataStr, PageVo.class);
        List<RecordsVo> records = Objects.requireNonNull(pageVo).getRecords();
        records.add(recordsVo);
        pageVo.setTotal(records.size());
        stringRedisTemplate.opsForValue().set(PREFIX + "page", objectMapper.writeValueAsString(pageVo));

        ErdOnlineModel erdOnlineModel = objectMapper.readValue(jsonObjectReq.toJSONString(), ErdOnlineModel.class);
        erdOnlineModel.setId(id);
        erdOnlineModel.setProjectName(recordsVo.getProjectName());
        erdOnlineModel.setType("1");
        stringRedisTemplate.opsForValue().set(PREFIX + id, objectMapper.writeValueAsString(erdOnlineModel));

        stringRedisTemplate.opsForValue().set(PREFIX + "load:" + id, "[]");

        return ResultVo.success("新建项目成功");
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/update", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<String> update(@RequestBody RecordsVo recordsVoReq) {
        String jsonDataStr = stringRedisTemplate.opsForValue().get(PREFIX + "page");
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
        stringRedisTemplate.opsForValue().set(PREFIX + "page", objectMapper.writeValueAsString(pageVo));

        jsonDataStr = stringRedisTemplate.opsForValue().get(PREFIX + "" + recordsVoReq.getId());
        ErdOnlineModel erdOnlineModel = objectMapper.readValue(jsonDataStr, ErdOnlineModel.class);
        Objects.requireNonNull(erdOnlineModel).setProjectName(recordsVoReq.getProjectName());
        stringRedisTemplate.opsForValue().set(PREFIX + recordsVoReq.getId(), objectMapper.writeValueAsString(erdOnlineModel));

        return ResultVo.success("更新项目成功");
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/save", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<Boolean> save(@RequestBody ErdOnlineModel erdOnlineModelReq) {
        String id = erdOnlineModelReq.getId();
        stringRedisTemplate.opsForValue().set(PREFIX + id, objectMapper.writeValueAsString(erdOnlineModelReq));
        return ResultVo.success(true);
    }


    @RequestMapping(value = "/ncnb/hisProject/load", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<JSONArray> load(@RequestBody JSONObject jsonObjectReq) {
        String projectId = jsonObjectReq.getString("projectId");
        String jsonStr = stringRedisTemplate.opsForValue().get(PREFIX + "load:" + projectId);
        JSONArray jsonArrayData = JSONObject.parseArray(jsonStr);
        return ResultVo.success(jsonArrayData);
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/connector/ping", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<String> ping(@RequestBody JSONObject jsonObjectReq) {
        String projectId = jsonObjectReq.getString("projectId");
        String jsonDataStr = stringRedisTemplate.opsForValue().get(PREFIX + projectId);
        ErdOnlineModel erdOnlineModel = objectMapper.readValue(jsonDataStr, ErdOnlineModel.class);
        DbsBean dbsBean = DbUtils.getDefaultDb(erdOnlineModel);
        Boolean success = DbUtils.checkDb(Objects.requireNonNull(dbsBean));
        if (success) {
            return ResultVo.success("连接成功:" + dbsBean.getProperties().getUrl());
        } else {
            return ResultVo.fail(dbsBean.getProperties().getUrl());
        }
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/project/refreshProjectModule", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<ModulesBean> refreshProjectModule(@RequestBody JSONObject jsonObjectReq) {
        String projectId = jsonObjectReq.getString("id");
        String name = jsonObjectReq.getString("moduleName");
        String jsonDataStr = stringRedisTemplate.opsForValue().get(PREFIX + projectId);
        ErdOnlineModel erdOnlineModel = objectMapper.readValue(jsonDataStr, ErdOnlineModel.class);
        ModulesBean modulesBean = ErdUtils.refreshModule(erdOnlineModel, name);
        stringRedisTemplate.opsForValue().set(PREFIX + projectId, objectMapper.writeValueAsString(erdOnlineModel));
        return ResultVo.success(modulesBean);
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/queryInfo/tree", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<List<Tree>> tree(String projectId) {
        String jsonDataStr = stringRedisTemplate.opsForValue().get(PREFIX + "tree:" + projectId);
        if (jsonDataStr == null) {
            jsonDataStr = "[]";
        }
        List<Tree> trees = objectMapper.readValue(jsonDataStr, new TypeReference<List<Tree>>() {
        });
        return ResultVo.success(trees);
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/queryInfo", method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<Boolean> queryInfo(@RequestBody JSONObject jsonObjectReq) {
        String projectId = jsonObjectReq.getString("projectId");
        Boolean isLeaf = jsonObjectReq.getBoolean("isLeaf");
        String title = jsonObjectReq.getString("title");
        Tree tree = new Tree();
        tree.setId(RandomUtils.nextLong(10000000000L, 99990000000L) + "");
        tree.setKey(tree.getId());
        tree.setValue(tree.getId());
        tree.setTitle(title);
        tree.setLabel(title);
        tree.setIsLeaf(isLeaf);
        List<Tree> trees = tree(projectId).getData();
        trees.add(tree);
        stringRedisTemplate.opsForValue().set(PREFIX + "tree:" + projectId, objectMapper.writeValueAsString(trees));
        stringRedisTemplate.opsForValue().set(PREFIX + "treeNode:" + tree.getId(), objectMapper.writeValueAsString(tree));
        return ResultVo.success(true);
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/queryInfo/{treeNodeId}", method = {RequestMethod.GET})
    public ResultVo<Tree> queryInfo(@PathVariable String treeNodeId) {
        String jsonStrData = stringRedisTemplate.opsForValue().get(PREFIX + "treeNode:" + treeNodeId);
        Tree tree = objectMapper.readValue(jsonStrData, Tree.class);
        return ResultVo.success(tree);
    }

    @SneakyThrows
    @RequestMapping(value = "ncnb/queryHistory", method = {RequestMethod.GET})
    public ResultVo<JSONObject> queryHistory() {
        JSONObject tableData = new JSONObject();
        tableData.put("records", Lists.newArrayList());
        tableData.put("total", 0);
        return ResultVo.success(tableData);
    }

    @SneakyThrows
    @RequestMapping(value = "/ncnb/queryInfo/{treeNodeId}", method = {RequestMethod.PUT})
    public ResultVo<Tree> queryInfoPut(@RequestBody JSONObject jsonObjectReq, @PathVariable String treeNodeId) {
        String jsonStrData = stringRedisTemplate.opsForValue().get(PREFIX + "treeNode:" + treeNodeId);
        Tree tree = objectMapper.readValue(jsonStrData, Tree.class);
        tree.setSqlInfo(jsonObjectReq.getString("sqlInfo"));
        stringRedisTemplate.opsForValue().set(PREFIX + "treeNode:" + treeNodeId, objectMapper.writeValueAsString(tree));
        return ResultVo.success(tree);
    }

    private static final Map<String, JdbcTemplate> MAP = Maps.newHashMap();

    @SneakyThrows
    @RequestMapping(value = {"/ncnb/queryInfo/exec", "/ncnb/queryInfo/explain"}, method = {RequestMethod.GET, RequestMethod.POST})
    public ResultVo<JSONObject> execExplain(@RequestBody JSONObject jsonObjectReq, HttpServletRequest request) {
        String dbName = jsonObjectReq.getString("dbName");
        String driverClassName = jsonObjectReq.getString("driverClassName");
        String key = jsonObjectReq.getString("key");
        String password = jsonObjectReq.getString("password");
        String queryId = jsonObjectReq.getString("queryId");
        String sql = jsonObjectReq.getString("sql");
        String url = jsonObjectReq.getString("url");
        String username = jsonObjectReq.getString("username");
        PropertiesBean propertiesBean = new PropertiesBean();
        propertiesBean.setDriver_class_name(driverClassName);
        propertiesBean.setUrl(url);
        propertiesBean.setPassword(password);
        propertiesBean.setUsername(username);

        JdbcTemplate jdbcTemplate = MAP.computeIfAbsent(dbName, s -> new JdbcTemplate(DbUtils.basicDataSource(propertiesBean)));
        sql = sql.replace(";", "");
        boolean explain = request.getRequestURI().contains("explain");
        if (explain) {
            sql = "desc " + sql;
            List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("columns", list.get(0).keySet());
            jsonObject.put("tableData", list);
            return ResultVo.success(jsonObject);
        }

        int fromIndex = sql.indexOf("from");
        String countSql = "SELECT count(*) " + sql.substring(fromIndex);
        Long count = jdbcTemplate.queryForObject(countSql, Long.class);
        if (!sql.contains("limit") && count >= 500) {
            sql += " limit 500";
        }
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        Set<String> columns;
        if (list.isEmpty()) {
            columns = Sets.newLinkedHashSet();
        } else {
            columns = list.get(0).keySet();
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("columns", columns);
        JSONObject tableData = new JSONObject();
        jsonObject.put("tableData", tableData);
        tableData.put("records", list);
        tableData.put("total", count);
        return ResultVo.success(jsonObject);
    }
}
