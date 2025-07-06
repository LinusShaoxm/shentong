package com.shentong.api.example;

import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class MD5Utils {

    /**
     * @param sk         深瞳智维平台分配的SK
     * @param dateString http header传的date字段，GMT时间
     * @return 动态密钥
     */
    public static String getMD5Hash(String sk, String dateString) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT")); // 设置时区为GMT
            long timeString = dateFormat.parse(dateString).getTime(); // 转换为毫秒

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update((sk + timeString).getBytes()); // md5(sk + 时间戳)
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}