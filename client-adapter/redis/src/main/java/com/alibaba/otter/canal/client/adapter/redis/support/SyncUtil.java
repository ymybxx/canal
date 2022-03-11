package com.alibaba.otter.canal.client.adapter.redis.support;

import com.alibaba.otter.canal.client.adapter.redis.config.RedisMappingConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public class SyncUtil {

    public static Map<String, String> getColumnsMap(RedisMappingConfig.RedisMapping redisMapping,
            Map<String, Object> data) {
        return getColumnsMap(redisMapping, data.keySet());
    }

    public static Map<String, String> getColumnsMap(RedisMappingConfig.RedisMapping redisMapping,
            Collection<String> columns) {
        Map<String, String> columnsMap;
        if (redisMapping.getMapAll()) {
            if (redisMapping.getAllMapColumns() != null) {
                return redisMapping.getAllMapColumns();
            }
            columnsMap = new LinkedHashMap<>();
            for (String srcColumn : columns) {
                boolean flag = true;
                if (redisMapping.getTargetColumns() != null) {
                    for (Map.Entry<String, String> entry : redisMapping.getTargetColumns()
                            .entrySet()) {
                        if (srcColumn.equals(entry.getValue())) {
                            columnsMap.put(entry.getKey(), srcColumn);
                            flag = false;
                            break;
                        }
                    }
                }
                if (flag) {
                    columnsMap.put(srcColumn, srcColumn);
                }
            }
            redisMapping.setAllMapColumns(columnsMap);
        } else {
            columnsMap = redisMapping.getTargetColumns();
        }
        return columnsMap;
    }

    public static String replaceStr(String source, String regex, List<String> toColumnList) {
        List<Integer> startIndexList = new ArrayList<>();
        int searchIndex = 0;
        while (true) {
            int start = source.indexOf(regex, searchIndex);
            if (start < 0) {
                break;
            }

            startIndexList.add(start);
            searchIndex = start + regex.length();
        }

        return replaceStr(source, startIndexList, regex.length(), toColumnList);
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

    public static Integer searchCount(String source, String target) {
        if (source == null || target == null) {
            return 0;
        }
        return StringUtils.countOccurrencesOf(source, target);
    }
}