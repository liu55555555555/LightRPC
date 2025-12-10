package com.Lrpc.compress;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CompressWrapper {
    private byte code;
    private String name;
    private Compressor compressor;
}
