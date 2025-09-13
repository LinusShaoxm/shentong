package com.shentong.api.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProvinceUtil {
    // 匹配以 "省/自治区/市" 结尾的字符串，并提取前面的部分
    private static final String GENERIC_PATTERN = "^(.+?)(?:省|自治区|市)$";

    public static String extractProvince(String filename) {
        Pattern pattern = Pattern.compile("(.+?)(?:省|自治区|市)");
        Matcher matcher = pattern.matcher(filename);
        if (matcher.find()) {
            return matcher.group(1); // 返回 "省/自治区/市" 前面的部分
        }
        return null;
    }
}