package com.rpcpostman.model.erd;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yudong
 * @date 2023/2/12
 */
@NoArgsConstructor
@Data
public class FromBean {
    private String entity;
    private String field;
}
