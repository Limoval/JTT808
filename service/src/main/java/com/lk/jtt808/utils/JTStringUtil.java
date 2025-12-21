package com.lk.jtt808.utils;

/*import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Slf4j
public class JTStringUtil {
    // 状态位标识
    public static final String[][] statusArray = new String[][]{
            new String[]{"ACC关", "ACC开"},// 0
            new String[]{"未定位", "定位"},// 1
            new String[]{"北纬", "南纬"},// 2
            new String[]{"东经", "西经"},// 3
            new String[]{"运营状态", "停运状态"},// 4
            new String[]{"经纬度未经保密插件保密", "经纬度已经保密插件保密"},// 5
            new String[]{"", "前撞预警"},// 6
            new String[]{"", "车道偏移预警"},// 7
            new String[]{"", ""},// 8
            new String[]{"", ""},// 9  00 空车 01半载 10保留 11满载
            new String[]{"车辆油路正常", "车辆油路断开"},// 10
            new String[]{"车辆电路正常", "车辆电路断开"},// 11
            new String[]{"车门解锁", "车门加锁"},// 12
            new String[]{"门1关(前门)", "门1开(前门)"},// 13
            new String[]{"门2关(中门)", "门2开(中门)"},// 14
            new String[]{"门3关(后门)", "门3开(后门)"},// 15
            new String[]{"门4关(驾驶席门)", "门4开(驾驶席门)"},// 16
            new String[]{"门5关(自定义)", "门5开(自定义)"},// 17
            new String[]{"未使用GPS卫星进行定位", "使用GPS卫星进行定位"},// 18
            new String[]{"未使用北斗卫星进行定位", "使用北斗卫星进行定位"},// 19
            new String[]{"未使用GLONASS卫星进行定位", "使用GLONASS卫星进行定位"},// 20
            new String[]{"未使用Galileo卫星进行定位", "使用Galileo卫星进行定位"},// 21
            new String[]{"车辆处于停止状态", "车辆处于行驶状态"},// 22
            new String[]{"", ""},// 23
            new String[]{"", ""},// 24
            new String[]{"", ""},// 25
            new String[]{"", ""},// 26
            new String[]{"", ""},// 27
            new String[]{"", ""},// 28
            new String[]{"", ""},// 29
            new String[]{"", ""},// 30
            new String[]{"", ""},// 31
    };

    public static final String[] alarmStrArray = new String[]{
            "紧急报警",
            "超速报警",
            "疲劳驾驶",
            "危险预警",
            "GNSS模块发生故障",
            "GNSS天线未接或被剪断",
            "GNSS天线短路",
            "终端主电源欠压",
            "终端主电源掉电",
            "终端LCD或显示器故障",
            "TTS模块故障",
            "摄像头故障",
            "道路运输证IC卡模块故障",
            "超速预警",
            "疲劳驾驶",
            "违规行驶报警",
            "胎压预警",
            "右转盲区异常报警",
            "当天累计驾驶超时",
            "超时停车",
            "进出区域报警",
            "进出路线报警",
            "路段行驶时间不足/过长",
            "路线偏离预警",
            "车辆VSS故障",
            "车辆油量异常",
            "车辆被盗",
            "车辆非法点火",
            "车辆非法位移",
            "碰撞预警",
            "侧翻预警",
            "非法开门报警",
    };

    public static final String[] ioStatus = {
            "深度休眠状态", "休眠状态"
    };

    *//**
     * 去除开头的0
     *//*
    public static String removeZero(String str) {
        return str.replaceFirst("^0*", "");
    }

    *//**
     * 省ID 是代号前两位
     *//*
    public static String provinceId2Str(int provinceId) {
        StringBuilder provinceStr = new StringBuilder();
        provinceStr.append(provinceId);
        while (provinceStr.length() < 2) {
            provinceStr.insert(0, 0);
        }
        return provinceStr.toString();
    }

    *//**
     * 区域ID 代号的后四位
     *//*
    public static String cityId2Str(int cityId) {
        StringBuilder cityStr = new StringBuilder();
        cityStr.append(cityId);
        while (cityStr.length() < 4) {
            cityStr.insert(0, 0);
        }
        return cityStr.toString();
    }


    *//**
     * alarm的32位 转换为对应的告警信息
     *
     * @param alarmInt 告警标识
     * @return 告警信息
     *//*
    public static String alarmInt2Str(long alarmInt) {
        String alarmStr = String.format("%32s", Long.toBinaryString(alarmInt)).replace(' ', '0');
        String reversedAlarmStr = new StringBuilder(alarmStr).reverse().toString();
        StringBuilder stringBuilder = new StringBuilder();
        // 高位到低位
        for (int i = 0; i < reversedAlarmStr.length(); i++) {
            if (reversedAlarmStr.charAt(i) == '1') {
                stringBuilder.append(alarmStrArray[i]);
                stringBuilder.append(";");
            }
        }
        // 获取报警
        return stringBuilder.toString();
    }


    *//**
     * 状态标识位32位 转换为对应的状态位信息。 从最高位第31位开始解析
     *
     * @param statusInt 状态标识
     * @return 状态位信息
     *//*
    public static String status2Str(long statusInt) {
        // 字符串都是补的空格
        String statusStr = String.format("%32s", Long.toBinaryString(statusInt)).replace(' ', '0');
        // 翻转字符串
        String reversedStatusStr = new StringBuilder(statusStr).reverse().toString();
        StringBuilder stringBuilder = new StringBuilder();
        // 高位到低位
        for (int i = 0; i < reversedStatusStr.length(); i++) {
            // 保留位
            if (i == 9 || i == 23 || i == 24 || i == 25 || i == 26
                    || i == 27 || i == 28 || i == 29 || i == 30 || i == 31) {
                continue;
            }
            // 如果是第8位特殊处理一下
            if (i == 8) {
                if (statusStr.charAt(i) == '0' && statusStr.charAt(i + 1) == '0') {
                    stringBuilder.append("空载");
                    stringBuilder.append(";");
                }
                if (statusStr.charAt(i) == '0' && statusStr.charAt(i + 1) == '1') {
                    stringBuilder.append("半载");
                    stringBuilder.append(";");
                }
                if (statusStr.charAt(i) == '1' && statusStr.charAt(i + 1) == '1') {
                    stringBuilder.append("满载");
                    stringBuilder.append(";");
                }
                continue;
            }
            if (statusStr.charAt(i) == '1') {
                stringBuilder.append(statusArray[i][1]);
                stringBuilder.append(";");
            } else {
                stringBuilder.append(statusArray[i][0]);
                stringBuilder.append(";");
            }
        }
        // 获取状态位标识
        return stringBuilder.toString();
    }

    *//**
     * 位置附加信息转换为字符串
     *//*
    public static void locationAppend2Str(byte state, ByteBuf msgBuf, TerminalMessage terminalMessage) {
        // 读取一个字节 代表数值有几位
        int valueLength = msgBuf.readUnsignedByte();
        switch (state) {
            // 里程信息
            case 0x01:
//                log.info("里程信息：{}", msgBuf.getUnsignedInt(msgBuf.readerIndex()));
                long mileage = msgBuf.readUnsignedInt();
                terminalMessage.setMileage(mileage);
                break;
            // 油量
            case 0x02:
                int fuel = msgBuf.readUnsignedShort();
                terminalMessage.setFuel(fuel * 10);
                break;
            // 行驶记录仪识别的速度
            case 0x03:
                int recordSpeed = msgBuf.readUnsignedShort() / 10;
                terminalMessage.setRecordSpeed(recordSpeed);
//                log.info("行驶记录仪识别的速度:{}", recordSpeed);
                break;
            // 需要人工确认的报警事件ID
            case 0x04:
                int alarmId = msgBuf.readUnsignedShort();
                terminalMessage.setAlarmId(alarmId);
                break;
//            // 卫星数
//            case 0x05:
//                break;
            // 车厢温度
            case 0x06:
                int tem = msgBuf.readUnsignedShort();
                terminalMessage.setTemperature(tem);
                break;
            // IO状态位
            case 0x2A:
                int io = msgBuf.readUnsignedShort();
                if (io < ioStatus.length) {
                    terminalMessage.setIoStatus(ioStatus[io]);
                }
                break;
            case 0x30:
                int single = msgBuf.readUnsignedByte();
                terminalMessage.setSingle(single);
                break;
            case 0x31:
                int satelliteNum = msgBuf.readUnsignedByte();
                terminalMessage.setSatelliteNum(satelliteNum);
                break;
            // 其他项暂时不解析
            default:
                msgBuf.readBytes(valueLength);
//                log.error("locationAppend2Str 未解析的附加项ID : {}", state);
//                msgBuf.readBytes(msgBuf.readableBytes());
                break;
        }
    }

    public static String getCurrentUtcDate() {
        // 转换为ZonedDateTime
        Instant nowUtc = Instant.now();
        ZonedDateTime now = ZonedDateTime.ofInstant(nowUtc, ZoneOffset.UTC);

        // 获取年、月、日、小时和分钟
        int year = now.getYear();
        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        int hour = now.getHour();
        int minute = now.getMinute();
        int second = now.getSecond();

        return String.valueOf(year).substring(2) +
                String.format("%02d", month) +
                String.format("%02d", day) +
                String.format("%02d", hour) +
                String.format("%02d", minute) +
                String.format("%02d", second);
    }
}*/
