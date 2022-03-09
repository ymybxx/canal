package com.alibaba.otter.canal.client.adapter.redis.service;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.TransportMode;

public class RedisService {

    private RedissonClient redissonClient;

    public RedisService(String hosts, Map<String, String> properties) {
        boolean isCluster = Boolean.parseBoolean(properties.get("cluster"));
        String password = properties.get("password");
        Config config = new Config();
        Codec codec = new JsonJacksonCodec();
        config.setCodec(codec);
        config.setTransportMode(TransportMode.NIO);
        String prefix = "redis://";
        /**
         * todo 哨兵模式未实现
         */
        if (isCluster) {
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            clusterServersConfig.addNodeAddress(hosts.split(","));
        } else {
            /**
             * todo ssl链接 timeout dataBase选择
             */
            SingleServerConfig singleServerConfig = config.useSingleServer();
            singleServerConfig.setAddress(prefix + hosts);
            if (StringUtils.isNotBlank(password)) {
                singleServerConfig.setPassword(password);
            }
        }

        redissonClient = Redisson.create(config);
    }

    public void close() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    public String set(String key, String value, Long expire) {
        if (expire == null || expire < 0) {
            return set(key, value);
        }
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.getAndSet(value, expire, TimeUnit.MILLISECONDS);
    }

    public String set(String key, String value) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.getAndSet(value);
    }

    public boolean delete(String key) {
        RBucket<String> bucket = redissonClient.getBucket(key);
        return bucket.delete();
    }
}