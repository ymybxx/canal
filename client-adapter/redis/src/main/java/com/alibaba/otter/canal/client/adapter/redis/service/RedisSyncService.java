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

            String nameSpace = redisMapping.getNamespace();
            if (CollectionUtils.isEmpty(pkList)) {
                logger.warn("nameSpace can not be null");
                return;
            }

            String key = redisMapping.getKey();
            if (SyncUtil.searchCount(key, "{}") != pkList.size()) {
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
                String resultKey = SyncUtil.replaceStr(key, "{}", keyList);
                resultKey = nameSpace + ":" + resultKey;
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