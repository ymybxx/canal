package com.alibaba.otter.canal.client.adapter.redis.support;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;
import com.alibaba.druid.stat.TableStat.Column;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang.StringUtils;

public class SqlUtil {

    public static List<String> getSelectColumnName(String sql) {
        if (StringUtils.isBlank(sql)) {
            return new ArrayList<>();
        }

        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
        MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
        List<String> columns = new ArrayList<>();
        for (SQLStatement sqlStatement : sqlStatements) {
            sqlStatement.accept(visitor);
            Collection<Column> visitorColumns = visitor.getColumns();
            for (Column column : visitorColumns) {
                columns.add(column.getName());
            }
        }

        return columns;
    }

}
