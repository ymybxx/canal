package com.alibaba.otter.canal.client.adapter.redis.support;

import com.alibaba.otter.canal.client.adapter.redis.config.RedisMappingConfig;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public class SyncUtil {

    public static Map<String, String> getColumnsMap(RedisMappingConfig.RedisMapping redisMapping) {
        Map<String, String> columnsMap = new LinkedHashMap<>();
        String sql = redisMapping.getSql();
        if (StringUtils.isEmpty(sql)) {
            return columnsMap;
        }

        List<String> selectColumnName = SqlUtil.getSelectColumnName(sql);
        if (selectColumnName.stream().anyMatch(column -> column.contains("*"))) {
            throw new RuntimeException("sql can not contain *");
        }

        Boolean lineToHump = redisMapping.getLineToHump();
        for (String srcColumn : selectColumnName) {
            if (lineToHump) {
                columnsMap.put(srcColumn, lineToHump(srcColumn));
            } else {
                columnsMap.put(srcColumn, srcColumn);
            }
        }
        return columnsMap;
    }

    public static String replaceStr(String source, List<Integer> startIndexList, Integer step,
            List<String> toColumnList) {
        String result = "";
        Integer start = 0;
        for (int i = 0; i < startIndexList.size(); i++) {
            Integer end = startIndexList.get(i);
            result = result + source.substring(start, end) + toColumnList.get(i);
            start = end + step;
        }

        return result;
    }

    public static String replaceStr(String pattern, Map<String, Object> dataMap) {
        List<String> keyList = getKeyListFromPattern(pattern);
        String result = pattern;
        for (String key : keyList) {
            Object value = dataMap.get(key);
            if (value == null) {
                throw new RuntimeException("can not find key:" + key + " in dataMap");
            }
            result = result.replace("{" + key + "}", value.toString());
        }
        return result;
    }

    public static String replaceStr(String pattern, ResultSet resultSet) throws SQLException {
        List<String> keyList = getKeyListFromPattern(pattern);
        String result = pattern;
        for (String key : keyList) {
            Object value = resultSet.getObject(key);
            if (value == null) {
                throw new RuntimeException("can not find key:" + key + " in dataMap");
            }
            result = result.replace("{" + key + "}", value.toString());
        }
        return result;
    }

    private static List<String> getKeyListFromPattern(String pattern) {
        List<String> keyList = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean flag = false;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '{') {
                flag = true;
            } else if (c == '}') {
                flag = false;
                keyList.add(sb.toString());
                sb = new StringBuilder();
            } else if (flag) {
                sb.append(c);
            }
        }
        return keyList;
    }

    private static String lineToHump(String str) {
        str = str.toLowerCase();
        StringBuilder sb = new StringBuilder();
        boolean upperCase = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                upperCase = true;
            } else if (upperCase) {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}