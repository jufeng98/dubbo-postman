package com.rpcpostman.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yudong
 * @date 2023/2/14
 */
@NoArgsConstructor
@Data
public class StatisticVo {
    private Integer yesterday;
    private Integer total;
    private Integer month;
    private Integer today;
}
