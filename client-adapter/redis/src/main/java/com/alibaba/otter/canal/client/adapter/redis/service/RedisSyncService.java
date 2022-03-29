package com.alibaba.otter.canal.client.adapter.redis.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.client.adapter.redis.config.RedisMappingConfig;
import com.alibaba.otter.canal.client.adapter.redis.support.SyncUtil;
import com.alibaba.otter.canal.client.adapter.support.Dml;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            String key = redisMapping.getKey();
            dml.getData().forEach((value) -> {
                String resultKey = SyncUtil.replaceStr(key, value);
                Map<String, Object> values = new HashMap<>();
                SyncUtil.getColumnsMap(redisMapping)
                        .forEach((srcColumn, targetColumn) -> values
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

    private void update(Map<String, String> dataMap, int expire) {
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