package com.alibaba.otter.canal.client.adapter.redis.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.client.adapter.redis.config.RedisMappingConfig;
import com.alibaba.otter.canal.client.adapter.redis.support.SyncUtil;
import com.alibaba.otter.canal.client.adapter.support.Dml;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class RedisSyncService {

    private static final Logger logger = LoggerFactory.getLogger(RedisSyncService.class);
    private RedisService redisService;

    public RedisSyncService(RedisService redisService) {
        this.redisService = redisService;
    }

    public void sync(Map<String, RedisMappingConfig> configMap, Dml dml) {
        if (configMap == null || configMap.values().isEmpty()) {
            return;
        }

        for (RedisMappingConfig config : configMap.values()) {
            RedisMappingConfig.RedisMapping redisMapping = config.getRedisMapping();
            String type = dml.getType();

            Map<String, String> dataMap = new HashMap<>();

            String pk = redisMapping.getPk();
            List<String> pkList = Arrays.asList(pk.split(","));
            if (CollectionUtils.isEmpty(pkList)) {
                logger.warn("pk can not be null");
                return;
            }
            String key = redisMapping.getKey();
            if (searchCount(key, "{}") != pkList.size()) {
                logger.warn("pk num don't match the key num");
                return;
            }

            dml.getData().forEach((value) -> {
                List<String> keyList = new ArrayList<>();
                for (String currentPk : pkList) {
                    Object pkValue = value.get(currentPk);
                    if (pkValue == null && currentPk != null) {
                        logger.warn("The pk value is not available: `{}`.`{}` column: {}",
                                redisMapping.getDatabase(), redisMapping.getTable(), currentPk);
                        return;
                    }

                    keyList.add(String.valueOf(pkValue));
                }
                String resultKey = replaceStr(key, "{}", keyList);
                Map<String, Object> values = new HashMap<>();
                SyncUtil.getColumnsMap(redisMapping, value)
                        .forEach((targetColumn, srcColumn) -> values
                                .put(targetColumn, value.get(srcColumn)));

                String jsonString = JSON.toJSONString(values);
                dataMap.put(resultKey, jsonString);
            });

            if (type != null && (type.equalsIgnoreCase("INSERT") || type
                    .equalsIgnoreCase("UPDATE"))) {
                update(dataMap, redisMapping.getExpire());
            } else if (type != null && type.equalsIgnoreCase("DELETE")) {
                delete(dataMap);
            }
        }
    }

    private String replaceStr(String source, String regex, List<String> toColumnList) {
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

    private String replaceStr(String source, List<Integer> startIndexList, Integer step,
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

    public Integer searchCount(String source, String target) {
        if (source == null || target == null) {
            return 0;
        }
        return StringUtils.countOccurrencesOf(source, target);
    }

    private void update(Map<String, String> dataMap, int expire) {
        //TODO 批量处理优化
        dataMap.forEach((key, value) -> {
            Object result;
            result = redisService.set(key, value, (long) expire);

            logger.info("set key: {} value: {} expire: {} result: {}", key, value, expire, result);
        });
    }

    private void delete(Map<String, String> dataMap) {
        String keys = String.join(" ", dataMap.keySet());
        boolean result = redisService.delete(keys);
        logger.info("del keys: {} result: {}", keys, result);
    }
}