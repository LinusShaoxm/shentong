package com.shentong.api.example;

import com.shentong.api.service.EncryptService;
import com.shentong.api.util.MD5Util;
import org.bouncycastle.util.encoders.Hex;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class App {
    public static void main(String[] args) {
        try {
            EncryptService encryptService = new EncryptService();

            String dateString = "Sun, 15 Aug 2021 10:00:00 GMT";
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // 明确指定时区

            Date date = sdf.parse(dateString);

            SM4Example sm = new SM4Example();
            String sk = "b0efd479020923419ff193ec0dbcb3dd1989898989";
            String hexKey = MD5Util.getMD5Hash(sk, dateString);
            byte[] key = Hex.decode(hexKey);//动态密钥

            //String plainText="uuid=11&userId=22&tenantId=33&channelId=4A&token=44&apiKey=55";//示例数据
            String plainText = "{\"userId\":\"\",\"knowledge\":[{\"knowledgeId\":\"111\",\"unitIds\":[\"111\"]},{\"knowledgeId\":\"222\",\"unitIds\":[\"333\"]}]}";
            byte[] input = plainText.getBytes();

            //加密后base64
            byte[] encrypted = sm.sm4Encrypt(key, input);
            String string = encryptService.sm4Encrypt(sk, date, plainText);
            String encodedString = Base64.getEncoder().encodeToString(encrypted);
            System.out.println("加密后base64:" + encodedString);
            System.out.println("加密后base64:" + string);

            //base64后解密
            byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
            byte[] decrypted = sm.sm4Decrypt(key, decodedBytes);
            String string1 = encryptService.sm4Decrypt(sk, date, encodedString);
            System.out.println("base64后解密:" + new String(decrypted));
            System.out.println("base64后解密:" + string1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}