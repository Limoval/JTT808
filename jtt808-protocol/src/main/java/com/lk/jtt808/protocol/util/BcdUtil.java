package com.lk.jtt808.protocol.util;

public class BcdUtil {
    /**
     * 将BCD编码的字节数组转换为字符串（如 0x01 0x23 0x45 -> "012345"）
     */
    public static String bcdToString(byte[] bcdBytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bcdBytes) {
            sb.append((b >> 4) & 0x0F); // 高4位
            sb.append(b & 0x0F);        // 低4位
        }
        return sb.toString();
    }

    /**
     * 将字符串转换为BCD编码的字节数组（如 "012345" -> 0x01 0x23 0x45）
     */
    public static byte[] stringToBcd(String str) {
        if (str.length() % 2 != 0) {
            str = "0" + str; // 补齐奇数位
        }
        byte[] bcdBytes = new byte[str.length() / 2];
        for (int i = 0; i < bcdBytes.length; i++) {
            int high = Character.digit(str.charAt(2 * i), 16);
            int low = Character.digit(str.charAt(2 * i + 1), 16);
            bcdBytes[i] = (byte) ((high << 4) | low);
        }
        return bcdBytes;
    }

    /**
     * 将字符串转换为固定长度的BCD编码字节数组。
     * <p>
     * 所需数字位数 = byteLength * 2
     * <ul>
     *   <li>str 长度不足：前面补零</li>
     *   <li>str 长度超过所需位数：直接抛出 IllegalArgumentException，不支持截断</li>
     * </ul>
     *
     * @param str        源字符串（纯数字）
     * @param byteLength 目标字节长度
     * @return BCD编码字节数组
     * @throws IllegalArgumentException 包含非数字字符或长度超长
     */
    public static byte[] stringToBcd(String str, int byteLength) {
        if (str == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }
        int requiredDigits = byteLength * 2;
        if (str.length() > requiredDigits) {
            throw new IllegalArgumentException(
                    "BCD string too long: expected max " + requiredDigits + " digits for " + byteLength
                            + " bytes, but got " + str.length() + " (value=" + str + ")");
        }
        // 校验纯数字
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("BCD string must contain only digits, got: " + str);
            }
        }
        // 前补零
        String padded = "0".repeat(requiredDigits - str.length()) + str;
        byte[] bcdBytes = new byte[byteLength];
        for (int i = 0; i < byteLength; i++) {
            int high = padded.charAt(2 * i) - '0';
            int low = padded.charAt(2 * i + 1) - '0';
            bcdBytes[i] = (byte) ((high << 4) | low);
        }
        return bcdBytes;
    }
}
