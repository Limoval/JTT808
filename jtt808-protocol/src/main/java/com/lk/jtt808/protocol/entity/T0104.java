package com.lk.jtt808.protocol.entity;

import com.lk.jtt808.protocol.annotation.MessageField;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.constant.JT808;
import com.lk.jtt808.protocol.entity.enums.DataType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 查询终端参数应答
 * 消息ID: 0x0104
 */
@MessageType(JT808.查询终端参数应答)
@EqualsAndHashCode(callSuper = true)
@Data
public class T0104 extends JT808Message implements JT808Response {

    @MessageField(order = 1, type = DataType.WORD, desc = "应答流水号")
    private int responseSerialNo;

    @MessageField(order = 2, type = DataType.BYTE, desc = "应答参数个数")
    private int parameterCount;

    @MessageField(order = 3, type = DataType.BYTES, desc = "参数项列表",
            converter = T8103.TerminalParameterListConverter.class)
    private List<T8103.TerminalParameter> parameters;
}
