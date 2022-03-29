package com.alibaba.otter.canal.client.adapter.redis.test;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat.Condition;
import com.alibaba.otter.canal.client.adapter.redis.support.SqlUtil;
import com.alibaba.otter.canal.client.adapter.redis.support.SyncUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class SqlParseTest {

    @Test
    public void testParseS2() {
        String sql = "select * from post";
        List<String> columnsFromSql = SqlUtil.getSelectColumnName(sql);
        System.out.println(columnsFromSql);
    }

    @Test
    public void testParseSql2() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("a", "1");
        testMap.put("b", "2");
        testMap.put("c", "3");
        String s = SyncUtil.replaceStr("{a}:{a}:{b}:{c}", testMap);
        System.out.println(s);
    }

    @Test
    public void testLineToHump() {
        String s = "test_name";
        System.out.println(lineToHump(s));
    }

    private String lineToHump(String str) {
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

    @Test
    public void testParseS() {
        List<String> columnsFromSql = getColumnsFromSql("select * from test");
        System.out.println(columnsFromSql);
        String sql = "select id from test where id = 1";
        columnsFromSql = getColumnsFromSql(sql);
        System.out.println(columnsFromSql);
    }

    public static List<String> getColumnsFromSql(String sql) {
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        List<String> columns = new ArrayList<>();
        for (SQLStatement sqlStatement : sqlStatements) {
            sqlStatement.accept(visitor);
            List<Condition> conditions = visitor.getConditions();
            for (Condition condition : conditions) {
                String name = condition.getColumn().getName();
                columns.add(name);
            }
        }

        return columns;
    }
}
