package com.rpcpostman.util;

import com.rpcpostman.model.erd.*;
import lombok.SneakyThrows;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/**
 * @author yudong
 * @date 2023/2/15
 */
public class DbUtils {
    public static BasicDataSource basicDataSource(PropertiesBean properties) {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(properties.getDriver_class_name());
        dataSource.setUrl(properties.getUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        dataSource.setInitialSize(1);
        dataSource.setMinIdle(1);
        dataSource.setDefaultAutoCommit(true);
        dataSource.setTestOnBorrow(true);
        return dataSource;
    }

    public static JdbcTemplate jdbcTemplate(DbsBean dbsBean) {
        PropertiesBean properties = dbsBean.getProperties();
        BasicDataSource dataSource = basicDataSource(properties);
        return new JdbcTemplate(dataSource);
    }

    public static DbsBean getDefaultDb(ErdOnlineModel erdOnlineModel) {
        List<DbsBean> dbs = erdOnlineModel.getProjectJSON().getProfile().getDbs();
        if (dbs == null) {
            throw new RuntimeException("请先保存并设置默认数据源");
        }
        DbsBean dbsBean = null;
        for (DbsBean db : dbs) {
            if (db.getDefaultDB()) {
                dbsBean = db;
            }
        }
        return dbsBean;
    }

    @SneakyThrows
    public static Boolean checkDb(DbsBean dbsBean) {
        if (!"MYSQL".equals(dbsBean.getSelect())) {
            throw new RuntimeException("目前只支持MySQL");
        }
        try {
            JdbcTemplate jdbcTemplate = jdbcTemplate(dbsBean);
            jdbcTemplate.execute("select now()");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static ModulesBean findModulesBean(String moduleCode, ErdOnlineModel erdOnlineModel) {
        ModulesBean module = null;
        for (ModulesBean modulesBean : erdOnlineModel.getProjectJSON().getModules()) {
            if (modulesBean.getName().equals(moduleCode)) {
                module = modulesBean;
                break;
            }
        }
        return module;
    }

}
