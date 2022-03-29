package com.alibaba.otter.canal.client.adapter.redis.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.otter.canal.client.adapter.redis.config.RedisMappingConfig;
import com.alibaba.otter.canal.client.adapter.redis.config.RedisMappingConfig.RedisMapping;
import com.alibaba.otter.canal.client.adapter.redis.support.SyncUtil;
import com.alibaba.otter.canal.client.adapter.support.AbstractEtlService;
import com.alibaba.otter.canal.client.adapter.support.AdapterConfig.AdapterMapping;
import com.alibaba.otter.canal.client.adapter.support.EtlResult;
import com.alibaba.otter.canal.client.adapter.support.Util;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.springframework.util.CollectionUtils;

public class RedisEtlService extends AbstractEtlService {

    private RedisService redisService;

    private RedisMappingConfig config;

    public RedisEtlService(RedisService redisService, RedisMappingConfig config) {
        super("Redis", config);
        this.redisService = redisService;
        this.config = config;
    }

    @Override
    protected boolean executeSqlImport(DataSource ds, String sql, List<Object> values,
            AdapterMapping mapping, AtomicLong impCount, List<String> errMsg) {
        try {
            RedisMapping redisMapping = (RedisMapping) mapping;

            Util.sqlRS(ds, sql, values, rs -> {
                int idx = 0;
                String pk = redisMapping.getPk();
                List<String> pkList = Arrays.asList(pk.split(","));
                if (CollectionUtils.isEmpty(pkList)) {
                    logger.warn("pk can not be null");
                    return false;
                }

                String key = redisMapping.getKey();

                Map<String, String> dataMap = new HashMap<>();
                while (true) {
                    try {
                        if (!rs.next()) {
                            break;
                        }

                        String resultKey = SyncUtil.replaceStr(key, rs);

                        Map<String, Object> jsonMap = new HashMap<>();
                        SyncUtil.getColumnsMap(redisMapping).forEach((srcColumn, targetColumn) -> {
                            try {
                                jsonMap.put(targetColumn, rs.getObject(srcColumn));
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                            }
                        });

                        String jsonString = JSON.toJSONString(jsonMap);
                        dataMap.put(resultKey, jsonString);
                        idx++;
                        impCount.incrementAndGet();
                    } catch (SQLException throwables) {
                        logger.error("Error while get data from srcDs", throwables);
                        break;
                    }
                }
                redisService.batchUpdate(dataMap);
                return idx;
            });
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    private List<String> getAllColumn(ResultSet rs) throws SQLException {
        int columnCount = rs.getMetaData().getColumnCount();
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            columns.add(rs.getMetaData().getColumnName(i + 1));
        }
        return columns;
    }

    public EtlResult importData(List<String> params) {
        RedisMapping redisMapping = config.getRedisMapping();
        String sql = redisMapping.getSql();
        return importData(sql, params);
    }
}