package com.alibaba.otter.canal.client.adapter.redis.config;

import com.alibaba.otter.canal.client.adapter.config.YmlConfigBinder;
import com.alibaba.otter.canal.client.adapter.support.MappingConfigsLoader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisConfigLoader {

    private static Logger logger = LoggerFactory.getLogger(RedisConfigLoader.class);

    /**
     * 加载 Redis 表映射配置
     *
     * @return 配置名/配置文件名--对象
     */
    public static Map<String, RedisMappingConfig> load(Properties envProperties) {
        logger.info("## Start loading redis mapping config ... ");

        Map<String, RedisMappingConfig> result = new LinkedHashMap<>();

        Map<String, String> configContentMap = MappingConfigsLoader.loadConfigs("redis");
        configContentMap.forEach((fileName, content) -> {
            RedisMappingConfig config = YmlConfigBinder
                    .bindYmlToObj(null, content, RedisMappingConfig.class, null, envProperties);
            if (config == null) {
                return;
            }
            try {
                config.validate();
            } catch (Exception e) {
                throw new RuntimeException("ERROR Config: " + fileName + " " + e.getMessage(), e);
            }
            result.put(fileName, config);
        });

        logger.info("## Redis mapping config loaded");
        return result;
    }
}
