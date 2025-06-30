package com.shentong.api.service;

import com.shentong.api.util.DateUtil;
import com.shentong.api.util.MD5Util;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

@Service
public class EncryptService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final int BLOCK_SIZE = 16;

    @PostConstruct
    public void init() {
        // 初始化检查
    }

    // PKCS7填充
    private byte[] pkcs7Padding(byte[] input) {
        int paddingSize = BLOCK_SIZE - (input.length % BLOCK_SIZE);
        byte[] padded = Arrays.copyOf(input, input.length + paddingSize);
        for (int i = input.length; i < padded.length; i++) {
            padded[i] = (byte) paddingSize;
        }
        return padded;
    }

    // PKCS7去除填充
    private byte[] pkcs7Unpadding(byte[] input) {
        int paddingSize = input[input.length - 1];
        return Arrays.copyOf(input, input.length - paddingSize);
    }

    // 生成动态密钥
    public byte[] generateDynamicKey(String sk, Date date) {
        String dateStr = DateUtil.formatGMT(date);
        String md5Hash = MD5Util.getMD5Hash(sk, dateStr);
        return Hex.decode(md5Hash);
    }

    // SM4加密
    public String sm4Encrypt(String sk, Date date, String plainText) {
        try {
            byte[] key = generateDynamicKey(sk, date);
            byte[] input = plainText.getBytes(StandardCharsets.UTF_8);
            
            SM4Engine sm4Engine = new SM4Engine();
            CipherParameters params = new KeyParameter(key);
            sm4Engine.init(true, params);
            
            byte[] paddedInput = pkcs7Padding(input);
            byte[] output = new byte[paddedInput.length];
            
            for (int i = 0; i < paddedInput.length; i += BLOCK_SIZE) {
                sm4Engine.processBlock(paddedInput, i, output, i);
            }
            
            return Base64.getEncoder().encodeToString(output);
        } catch (Exception e) {
            throw new RuntimeException("SM4加密失败", e);
        }
    }

    // SM4解密
    public String sm4Decrypt(String sk, Date date, String encryptedText) {
        try {
            byte[] key = generateDynamicKey(sk, date);
            byte[] input = Base64.getDecoder().decode(encryptedText);
            
            SM4Engine sm4Engine = new SM4Engine();
            CipherParameters params = new KeyParameter(key);
            sm4Engine.init(false, params);
            
            byte[] output = new byte[input.length];
            
            for (int i = 0; i < input.length; i += BLOCK_SIZE) {
                sm4Engine.processBlock(input, i, output, i);
            }
            
            byte[] unpadded = pkcs7Unpadding(output);
            return new String(unpadded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("SM4解密失败", e);
        }
    }
}