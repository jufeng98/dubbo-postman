package com.rpcpostman.model.erd;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author yudong
 * @date 2023/2/12
 */
@NoArgsConstructor
@Data
public class GraphCanvasBean {
    private List<NodesBean> nodes;
    private List<EdgesBean> edges;
}
