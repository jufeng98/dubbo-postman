package com.rpcpostman.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yudong
 * @date 2023/2/14
 */
@NoArgsConstructor
@Data
public class RecordsVo {
    private String id;
    private String projectName;
    private String tags;
    private String description;
    private String type;
}
