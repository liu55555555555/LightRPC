package com.Lrpc.utils.zookeeper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZookeeperNode {
    private String path;
    private byte[] data;
}
