package com.limou.hrms.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
@Slf4j
public class DatabaseInitConfig {

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        try (Connection connection = dataSource.getConnection()) {
            ensureDepartmentIdColumn(connection);
        } catch (SQLException e) {
            log.error("Database init failed", e);
        }
    }

    private void ensureDepartmentIdColumn(Connection connection) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        java.sql.ResultSet columns = metaData.getColumns(null, null, "approval_record", "departmentId");
        
        if (!columns.next()) {
            log.info("Adding departmentId column to approval_record table");
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE approval_record ADD COLUMN departmentId BIGINT(20) DEFAULT NULL COMMENT '目标部门ID（用于部门负责人权限匹配）' AFTER status");
                log.info("Successfully added departmentId column");
            }
        }
        columns.close();
    }
}