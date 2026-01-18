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
}
