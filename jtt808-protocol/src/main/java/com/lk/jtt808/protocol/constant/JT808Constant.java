package com.lk.jtt808.protocol.constant;

import java.nio.charset.Charset;

/**
 * @author : lyp
 * @Description: JT808协议中的相关常量
 */
public class JT808Constant {

    /**
     * 默认字符集为GBK
     */
    public static final Charset DEFAULT_CHARSET = Charset.forName("GBK");

    /**
     * 消息分隔符(0x表示16进制)
     */
    public static final byte PKG_DELIMITER = 0x7e;

    /**
     * 消息分隔符(首尾标志位7e)
     */
    public static final String HEAD_TAIL_FLAGS = Integer.toHexString(PKG_DELIMITER);

    /**
     * 最小包头长度(2013年协议为12位, 2019年协议为16位)
     */
    public static final int MIN_PACKET_HEADER_NUMBER = 12;

    /**
     * 最大数据包的长度, 如果超过需要分包发送
     */
    public static final int PACKAGE_MAX_LENGTH = 1024;

    /**
     * 1. 终端通用应答
     */
    public static final int 终端通用应答 = 0x001;

    /**
     * 2. 平台通用应答
     */
    public static final int 平台通用应答 = 0x8001;

    /**
     * 3. 终端心跳
     */
    public static final int 终端心跳 = 0x0002;

    /**
     * 4. 服务器补传分包请求
     */
    public static final int 服务器补传分包请求 = 0x8003;

    /**
     * 5. 终端补传分包请求
     */
    public static final int 终端补传分包请求 = 0x0005;

    /**
     * 6. 终端注册
     */
    public static final int 终端注册 = 0x0100;

    /**
     * 7. 终端注册应答
     */
    public static final int 终端注册应答 = 0x8100;

    /**
     * 8. 终端注销
     */
    public static final int 终端注销 = 0x0003;

    /**
     * 9. 查询服务器时间
     */
    public static final int 查询服务器时间 = 0x0004;

    /**
     * 10. 终端鉴权
     */
    public static final int 终端鉴权 = 0x0102;

    /**
     * 11. 设置终端参数
     */
    public static final int 设置终端参数 = 0x8103;

    /**
     * 12. 查询终端参数
     */
    public static final int 查询终端参数 = 0x8104;

    /**
     * 13. 查询终端参数应答
     */
    public static final int 查询终端参数应答 = 0x0104;

    /**
     * 14. 终端控制
     */
    public static final int 终端控制 = 0x8105;

    /**
     * 15. 查询指定终端参数
     */
    public static final int 查询指定终端参数 = 0x8106;

    /**
     * 16. 查询终端属性
     */
    public static final int 查询终端属性 = 0x8107;

    /**
     * 17. 查询终端属性应答
     */
    public static final int 查询终端属性应答 = 0x0107;

    /**
     * 18. 下发终端升级包
     */
    public static final int 下发终端升级包 = 0x8108;

    /**
     * 19. 终端升级结果通知
     */
    public static final int 终端升级结果通知 = 0x0108;

    /**
     * 20. 位置信息汇报
     */
    public static final int 位置信息汇报 = 0x0200;

    /**
     * 21. 位置信息查询
     */
    public static final int 位置信息查询 = 0x8201;

    /**
     * 22. 位置信息查询应答
     */
    public static final int 位置信息查询应答 = 0x0201;

    /**
     * 23. 临时位置跟踪控制
     */
    public static final int 临时位置跟踪控制 = 0x8202;

    /**
     * 24. 事件设置
     */
    public static final int 事件设置 = 0x8301;

    /**
     * 25. 事件报告
     */
    public static final int 事件报告 = 0x0301;

    /**
     * 26. 提问下发
     */
    public static final int 提问下发 = 0x8302;

    /**
     * 27. 提问应答
     */
    public static final int 提问应答 = 0x0302;

    /**
     * 28. 信息点播菜单设置
     */
    public static final int 信息点播菜单设置 = 0x8303;

    /**
     * 29. 信息点播/取消
     */
    public static final int 信息点播取消 = 0x0303;

    /**
     * 30. 信息服务
     */
    public static final int 信息服务 = 0x8304;

    /**
     * 31. 查询服务器时间应答
     */
    public static final int 查询服务器时间应答 = 0x8004;

    /**
     * 32. 电话回拨
     */
    public static final int 电话回拨 = 0x8400;

    /**
     * 33. 设置电话本
     */
    public static final int 设置电话本 = 0x8401;

    /**
     * 34. 车辆控制
     */
    public static final int 车辆控制 = 0x8500;

    /**
     * 35. 车辆控制应答
     */
    public static final int 车辆控制应答 = 0x0500;

    /**
     * 36. 设置圆形区域
     */
    public static final int 设置圆形区域 = 0x8600;

    /**
     * 37. 删除圆形区域
     */
    public static final int 删除圆形区域 = 0x8601;

    /**
     * 38. 设置矩形区域
     */
    public static final int 设置矩形区域 = 0x8602;

    /**
     * 39. 删除矩形区域
     */
    public static final int 删除矩形区域 = 0x8603;

    /**
     * 40. 设置多边形区域
     */
    public static final int 设置多边形区域 = 0x8604;

    /**
     * 41. 删除多边形区域
     */
    public static final int 删除多边形区域 = 0x8605;

    /**
     * 42. 设置路线
     */
    public static final int 设置路线 = 0x8606;

    /**
     * 43. 删除路线
     */
    public static final int 删除路线 = 0x8607;

    /**
     * 44. 行驶记录仪数据采集命令
     */
    public static final int 行驶记录仪数据采集命令 = 0x8700;

    /**
     * 45. 行驶记录仪数据上传
     */
    public static final int 行驶记录仪数据上传 = 0x0700;

    /**
     * 46. 行驶记录仪参数下传命令
     */
    public static final int 行驶记录仪参数下传命令 = 0x8701;

    /**
     * 47. 人工确认报警消息
     */
    public static final int 人工确认报警消息 = 0x8203;

    /**
     * 48. 服务器向终端发起链路检测请求
     */
    public static final int 服务器向终端发起链路检测请求 = 0x8204;

    /**
     * 49. 文本信息下发
     */
    public static final int 文本信息下发 = 0x8300;

    /**
     * 50. 驾驶员身份信息采集上报
     */
    public static final int 驾驶员身份信息采集上报 = 0x0702;

    /**
     * 51. 上报驾驶员身份信息请求
     */
    public static final int 上报驾驶员身份信息请求 = 0x8702;

    /**
     * 52. 定位数据批量上传
     */
    public static final int 定位数据批量上传 = 0x0704;

    /**
     * 53. CAN总线数据上传
     */
    public static final int CAN总线数据上传 = 0x0705;

    /**
     * 54. 多媒体事件信息上传
     */
    public static final int 多媒体事件信息上传 = 0x0800;

    /**
     * 55. 多媒体数据上传
     */
    public static final int 多媒体数据上传 = 0x0801;

    /**
     * 56. 多媒体数据上传应答
     */
    public static final int 多媒体数据上传应答 = 0x8800;

    /**
     * 57. 摄像头立即拍摄命令
     */
    public static final int 摄像头立即拍摄命令 = 0x8801;

    /**
     * 58. 摄像头立即拍摄命令应答
     */
    public static final int 摄像头立即拍摄命令应答 = 0x0805;

    /**
     * 59. 存储多媒体数据检索
     */
    public static final int 存储多媒体数据检索 = 0x8802;

    /**
     * 60. 存储多媒体数据检索应答
     */
    public static final int 存储多媒体数据检索应答 = 0x0802;

    /**
     * 61. 查询区域或线路数据
     */
    public static final int 查询区域或线路数据 = 0x8608;

    /**
     * 62. 查询区域或线路数据应答
     */
    public static final int 查询区域或线路数据应答 = 0x0608;

    /**
     * 63. 电子运单上报
     */
    public static final int 电子运单上报 = 0x0701;

    /**
     * 64. 存储多媒体数据上传
     */
    public static final int 存储多媒体数据上传 = 0x8803;

    /**
     * 65. 录音开始命令
     */
    public static final int 录音开始命令 = 0x8804;

    /**
     * 66. 单条存储多媒体数据检索上传命令
     */
    public static final int 单条存储多媒体数据检索上传命令 = 0x8805;

    /**
     * 67. 数据下行透传
     */
    public static final int 数据下行透传 = 0x8900;

    /**
     * 68. 数据上行透传
     */
    public static final int 数据上行透传 = 0x0900;

    /**
     * 69. 数据压缩上报
     */
    public static final int 数据压缩上报 = 0x0901;

    /**
     * 70. 平台RSA公钥
     */
    public static final int 平台RSA公钥 = 0x8A00;

    /**
     * 71. 终端RSA公钥
     */
    public static final int 终端RSA公钥 = 0x0A00;

}
