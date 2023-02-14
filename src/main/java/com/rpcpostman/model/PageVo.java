package com.rpcpostman.model;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yudong
 * @date 2023/2/14
 */
@NoArgsConstructor
@Data
public class PageVo {

    private List<RecordsVo> records;
    private Integer total;
    private Integer size;
    private Integer current;
    private List<JSONObject> orders;
    private Boolean searchCount;
    private Integer pages;

}
