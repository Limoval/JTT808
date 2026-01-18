package com.lk.jtt808.device.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AuthCodeGenerator {

    public static String generateAuthCode(String deviceId, String secretKey) {
        String rawData = deviceId + secretKey; // 按实际规则拼接
        return md5(rawData);
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes());
            return bytesToHex(hashBytes).toUpperCase(); // 转为大写HEX
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
