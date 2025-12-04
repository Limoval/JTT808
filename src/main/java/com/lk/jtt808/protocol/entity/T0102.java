package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.entity.enums.DataType;
import com.lk.jtt808.utils.JT808;
import lombok.Data;

/**
 * @author: Limoval
 * time: 2025/5/23 16:09 周五
 * description:
 */
@MessageType(JT808.终端鉴权)
@Data
public class T0102 extends JT808Message {

    @MessageField(order = 1, type = DataType.STRING, desc = "鉴权码")
    private String authCode;

}
