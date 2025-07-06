package com.shentong.api.example;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.params.KeyParameter;

import java.security.Security;
import java.util.Arrays;

public class SM4Example {
    static {
        // 添加 BouncyCastle 提供程序sm4Decrypt
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final int BLOCK_SIZE = 16; // SM4 块大小

    // PKCS7 填充
    public static byte[] pkcs7Padding(byte[] input) {
        int paddingSize = BLOCK_SIZE - (input.length % BLOCK_SIZE);
        byte[] padded = Arrays.copyOf(input, input.length + paddingSize);
        for (int i = input.length; i < padded.length; i++) {
            padded[i] = (byte) paddingSize;
        }
        return padded;
    }

    // PKCS7 去除填充
    public static byte[] pkcs7Unpadding(byte[] input) {
        int paddingSize = input[input.length - 1];
        return Arrays.copyOf(input, input.length - paddingSize);
    }

    // SM4 加密
    public static byte[] sm4Encrypt(byte[] key, byte[] input) {
        SM4Engine sm4Engine = new SM4Engine();
        CipherParameters params = new KeyParameter(key);
        sm4Engine.init(true, params); // 初始化为加密模式

        byte[] paddedInput = pkcs7Padding(input);
        byte[] output = new byte[paddedInput.length];

        for (int i = 0; i < paddedInput.length; i += BLOCK_SIZE) {
            sm4Engine.processBlock(paddedInput, i, output, i);
        }

        return output;
    }

    // SM4 解密
    public static byte[] sm4Decrypt(byte[] key, byte[] input) {
        SM4Engine sm4Engine = new SM4Engine();
        CipherParameters params = new KeyParameter(key);
        sm4Engine.init(false, params); // 初始化为解密模式

        byte[] output = new byte[input.length];

        for (int i = 0; i < input.length; i += BLOCK_SIZE) {
            sm4Engine.processBlock(input, i, output, i);
        }

        return pkcs7Unpadding(output);
    }
}