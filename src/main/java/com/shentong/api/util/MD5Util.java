// src/main/java/com/example/deepvision/util/MD5Util.java
package com.shentong.api.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MD5Util {

    public static String getMD5Hash(String sk, String dateString) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date date = dateFormat.parse(dateString);
            long time = date.getTime();
            
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((sk + time).getBytes());
            
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5哈希计算失败", e);
        }
    }
}