package com.lk.jtt808.protocol.entity.enums;

import lombok.Getter;

@Getter
public enum DataType {
    BYTE,       // 1字节
    WORD,       // 2字节无符号
    DWORD,      // 4字节无符号
    BCD,        // BCD编码
    STRING,     // 定长/变长字符串
    BYTES,      // 原始字节
    LIST,           // 列表类型
    OBJECT,         // 嵌套对象类型
    CONDITIONAL,    // 条件字段（根据前面字段值决定是否存在）
    VARIABLE_LENGTH // 可变长度字段（读取剩余所有字节）
}