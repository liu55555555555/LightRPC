package com.Lrpc.serialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class SerializeWrapper {
    private Serialize serialize;
    private byte code;
    private String serializeName;
}
