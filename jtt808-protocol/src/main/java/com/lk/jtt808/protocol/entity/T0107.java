package com.lk.jtt808.protocol.entity;

import cn.hutool.core.util.HexUtil;
import com.lk.jtt808.protocol.annotation.CustomMapping;
import com.lk.jtt808.protocol.annotation.MessageType;
import com.lk.jtt808.protocol.util.BcdUtil;
import com.lk.jtt808.protocol.constant.JT808;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@MessageType(JT808.查询终端属性应答)
@Data
@Slf4j
public class T0107 extends JT808Message implements JT808Response, CustomMapping {

    //应答流水号（对应平台下发的 outboundSerialNo）
    private int responseSerialNo;

    //终端类型
    private int terminalType;
    //制造商ID
    private byte[] manufacturerId;
    //终端型号
    private byte[] terminalModel;
    //终端ID
    private byte[] terminalId;
    //ICCID
    private String iccid;
    //终端硬件版本号长度
    private int hardwareVersionLength;
    //终端硬件版本号
    private String hardwareVersion;
    //终端固件版本号长度
    private int firmwareVersionLength;
    //终端版本号
    private String firmwareVersion;
    //GNSS模块属性
    private int gnssAttribute;
    //通信模块属性
    private int commAttribute;

    @Override
    public boolean customParse(ByteBuf buf) {
        try {
            // 解析应答流水号（2字节WORD）
            this.responseSerialNo = buf.readUnsignedShort();

            // 解析固定长度字段
            this.terminalType = buf.readUnsignedShort();

            this.manufacturerId = new byte[5];
            buf.readBytes(this.manufacturerId);

            this.terminalModel = new byte[20];
            buf.readBytes(this.terminalModel);

            this.terminalId = new byte[7];
            buf.readBytes(this.terminalId);

            byte[] bcdBytes = new byte[10];
            buf.readBytes(bcdBytes);
            this.iccid = BcdUtil.bcdToString(bcdBytes);

            this.hardwareVersionLength = buf.readUnsignedByte();
            if (this.hardwareVersionLength > 0) {
                byte[] hwVersionBytes = new byte[this.hardwareVersionLength];
                buf.readBytes(hwVersionBytes);
                this.hardwareVersion = new String(hwVersionBytes, StandardCharsets.UTF_8);
            }

            this.firmwareVersionLength = buf.readUnsignedByte();
            if (this.firmwareVersionLength > 0) {
                byte[] fwVersionBytes = new byte[this.firmwareVersionLength];
                buf.readBytes(fwVersionBytes);
                this.firmwareVersion = new String(fwVersionBytes, StandardCharsets.UTF_8);
            }

            // 解析剩余固定字段
            this.gnssAttribute = buf.readUnsignedByte();
            this.commAttribute = buf.readUnsignedByte();

            log.debug("T0107自定义解析成功");
            return true;

        } catch (Exception e) {
            log.error("T0107自定义解析失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean customEncode(ByteBuf buf) {
        try {
            buf.writeShort(this.terminalType);
            buf.writeBytes(this.manufacturerId);
            buf.writeBytes(this.terminalModel);
            buf.writeBytes(this.terminalId);

            if (this.iccid != null) {
                byte[] iccidBytes = BcdUtil.stringToBcd(this.iccid);
                buf.writeBytes(iccidBytes);
            } else {
                buf.writeZero(10);
            }

            if (this.hardwareVersion != null) {
                byte[] hwBytes = this.hardwareVersion.getBytes(StandardCharsets.UTF_8);
                buf.writeByte(hwBytes.length);
                buf.writeBytes(hwBytes);
            } else {
                buf.writeByte(0);
            }

            if (this.firmwareVersion != null) {
                byte[] fwBytes = this.firmwareVersion.getBytes(StandardCharsets.UTF_8);
                buf.writeByte(fwBytes.length);
                buf.writeBytes(fwBytes);
            } else {
                buf.writeByte(0);
            }

            buf.writeByte(this.gnssAttribute);
            buf.writeByte(this.commAttribute);

            log.debug("T0107自定义编码成功");
            return true;

        } catch (Exception e) {
            log.error("T0107自定义编码失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // 获取制造商ID的字符串表示
    public String getManufacturerIdString() {
        return manufacturerId != null ?
                new String(manufacturerId, StandardCharsets.UTF_8).trim() : "";
    }

    // 获取终端型号的字符串表示
    public String getTerminalModelString() {
        return terminalModel != null ?
                new String(terminalModel, StandardCharsets.UTF_8).trim() : "";
    }

    // 获取终端ID的字符串表示
    public String getTerminalIdString() {
        return terminalId != null ?
                new String(terminalId, StandardCharsets.UTF_8).trim() : "";
    }

    // 获取制造商ID的16进制表示
    public String getManufacturerIdHex() {
        return manufacturerId != null ?
                HexUtil.encodeHexStr(manufacturerId).toUpperCase() : "";
    }

    // 获取终端型号的16进制表示
    public String getTerminalModelHex() {
        return terminalModel != null ?
                HexUtil.encodeHexStr(terminalModel).toUpperCase() : "";
    }

    // 获取终端ID的16进制表示
    public String getTerminalIdHex() {
        return terminalId != null ?
                HexUtil.encodeHexStr(terminalId).toUpperCase() : "";
    }

    @Override
    public int getResponseSerialNo() {
        return this.responseSerialNo;
    }

    public String toReadableString() {
        return "T0107[" +
                "terminalType=" + terminalType +
                ", manufacturerId='" + getManufacturerIdString() + "'" +
                "(" + getManufacturerIdHex() + ")" +
                ", terminalModel='" + getTerminalModelString() + "'" +
                "(" + getTerminalModelHex() + ")" +
                ", terminalId='" + getTerminalIdString() + "'" +
                "(" + getTerminalIdHex() + ")" +
                ", iccid='" + iccid + "'" +
                ", hardwareVersion='" + hardwareVersion + "'" +
                ", firmwareVersion='" + firmwareVersion + "'" +
                ", gnssAttribute=" + gnssAttribute +
                ", commAttribute=" + commAttribute +
                "]";
    }

    @Override
    public String toString() {
        return toReadableString();
    }

}