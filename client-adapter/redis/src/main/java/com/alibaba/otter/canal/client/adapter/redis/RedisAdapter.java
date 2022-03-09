package com.alibaba.otter.canal.client.adapter.redis;

import com.alibaba.otter.canal.client.adapter.OuterAdapter;
import com.alibaba.otter.canal.client.adapter.redis.config.RedisConfigLoader;
import com.alibaba.otter.canal.client.adapter.redis.config.RedisMappingConfig;
import com.alibaba.otter.canal.client.adapter.redis.service.RedisService;
import com.alibaba.otter.canal.client.adapter.redis.service.RedisSyncService;
import com.alibaba.otter.canal.client.adapter.support.Dml;
import com.alibaba.otter.canal.client.adapter.support.OuterAdapterConfig;
import com.alibaba.otter.canal.client.adapter.support.SPI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Redis 适配器实现类
 *
 * @author ivothgle 2019-04-18
 * @version 1.0.0
 */
@SPI("redis")
public class RedisAdapter implements OuterAdapter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<String, RedisMappingConfig> redisMapping = new ConcurrentHashMap<>();                    // 文件名对应配置
    private Map<String, Map<String, RedisMappingConfig>> mappingConfigCache = new ConcurrentHashMap<>(); // 库名-表名对应配置


    private RedisSyncService redisSyncService;
    private Properties envProperties;
    private RedisService redisService;

    @Override
    public void init(OuterAdapterConfig configuration, Properties envProperties) {
        this.envProperties = envProperties;

        Map<String, RedisMappingConfig> redisMappingTmp = RedisConfigLoader.load(envProperties);
        // 过滤不匹配的key的配置
        redisMappingTmp.forEach((key, mappingConfig) -> {
            if ((mappingConfig.getOuterAdapterKey() == null && configuration.getKey() == null)
                    || (mappingConfig.getOuterAdapterKey() != null
                    && mappingConfig.getOuterAdapterKey()
                    .equalsIgnoreCase(configuration.getKey()))) {
                redisMapping.put(key, mappingConfig);
            }
        });

        if (redisMapping.isEmpty()) {
            throw new RuntimeException(
                    "No redis adapter found for config key: " + configuration.getKey());
        }

        Map<String, String> properties = configuration.getProperties();

        for (Map.Entry<String, RedisMappingConfig> entry : redisMapping.entrySet()) {
            String configName = entry.getKey();
            RedisMappingConfig mappingConfig = entry.getValue();

            String key = StringUtils.trimToEmpty(mappingConfig.getDestination()) + "-"
                    + (isTCPMode() ? "" : StringUtils.trimToEmpty(mappingConfig.getGroupId()) + "_")
                    + mappingConfig.getRedisMapping().getDatabase() + "-"
                    + mappingConfig.getRedisMapping().getTable();

            Map<String, RedisMappingConfig> configMap = mappingConfigCache
                    .computeIfAbsent(key, k1 -> new ConcurrentHashMap<>());
            configMap.put(configName, mappingConfig);
        }

        redisService = new RedisService(configuration.getHosts(), properties);

        redisSyncService = new RedisSyncService(redisService);
    }

    public void sync(List<Dml> dmls) {
        if (dmls == null || dmls.isEmpty()) {
            return;
        }

        for (Dml dml : dmls) {
            if (!dml.getIsDdl()) {
                Map<String, RedisMappingConfig> configMap = mappingConfigCache.get(
                        StringUtils.trimToEmpty(
                                dml.getDestination()) + "-"
                                + (isTCPMode() ? ""
                                : StringUtils.trimToEmpty(dml.getGroupId()) + "_")
                                + dml.getDatabase() + "-"
                                + dml.getTable()
                );

                redisSyncService.sync(configMap, dml);
            }
        }
    }

    private boolean isTCPMode() {
        return envProperties != null && "tcp"
                .equalsIgnoreCase(envProperties.getProperty("canal.conf.mode"));
    }

    @Override
    public String getDestination(String task) {
        RedisMappingConfig config = redisMapping.get(task);
        if (config != null) {
            return config.getDestination();
        }
        return null;
    }

    @Override
    public void destroy() {
        if (redisService != null) {
            redisService.close();
        }
    }
}