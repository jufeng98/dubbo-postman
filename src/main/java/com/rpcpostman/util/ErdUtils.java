package com.rpcpostman.util;

import com.google.common.collect.Lists;
import com.rpcpostman.model.erd.*;
import static com.rpcpostman.util.DbUtils.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yudong
 * @date 2023/2/12
 */
@Slf4j
public class ErdUtils {

    @SneakyThrows
    public static ModulesBean refreshModule(ErdOnlineModel erdOnlineModel, String name) {
        DbsBean dbsBean = getDefaultDb(erdOnlineModel);
        if (dbsBean == null) {
            throw new RuntimeException("请先保存并设置默认数据源");
        }
        ModulesBean modulesBean = findModulesBean(name, erdOnlineModel);
        List<DatatypeBean> datatypeBeans = erdOnlineModel.getProjectJSON().getDataTypeDomains().getDatatype();
        BasicDataSource dataSource = basicDataSource(dbsBean.getProperties());
        JdbcTemplate jdbcTemplate = jdbcTemplate(dbsBean);
        Connection connection = dataSource.getConnection();
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        List<EntitiesBean> entitiesBeans = modulesBean.getEntities().stream()
                .map(entitiesBean -> tableToEntity(entitiesBean, databaseMetaData, datatypeBeans, jdbcTemplate))
                .collect(Collectors.toList());
        modulesBean.setEntities(entitiesBeans);
        connection.close();
        return modulesBean;
    }

    public static EntitiesBean tableToEntity(EntitiesBean entitiesBean, DatabaseMetaData databaseMetaData,
                                             List<DatatypeBean> datatypeBeans, JdbcTemplate jdbcTemplate) {
        String tableName = entitiesBean.getTitle();
        Table table = getTableInfo(tableName, databaseMetaData);
        if (table == null) {
            return entitiesBean;
        }
        entitiesBean.setChnname(table.getRemarks());
        List<Column> columns = getTableColumns(tableName, databaseMetaData);
        entitiesBean.setFields(toFieldsBeans(columns, datatypeBeans));
        List<IndexsBean> indexsBeans = getTableIndex(tableName, databaseMetaData);
        entitiesBean.setIndexs(indexsBeans);
        List<Map<String, Object>> list = jdbcTemplate.queryForList("show create table " + tableName);
        entitiesBean.setOriginalCreateTableSql(list.get(0).get("Create Table").toString());
        return entitiesBean;
    }

    public static List<FieldsBean> toFieldsBeans(List<Column> columns, List<DatatypeBean> datatypeBeans) {
        return columns.stream()
                .map(column -> {
                    FieldsBean fieldsBean = new FieldsBean();
                    if (column.getRemarks().length() > 12) {
                        fieldsBean.setChnname(column.getRemarks().substring(0, 12) + "...");
                    } else {
                        fieldsBean.setChnname(column.getRemarks());
                    }
                    fieldsBean.setName(column.getName());
                    String columnTypeName = column.getTypeName();
                    DatatypeBean datatypeBean = findDatatypeBean(columnTypeName, datatypeBeans);
                    fieldsBean.setTypeName(datatypeBean.getName());
                    fieldsBean.setType(datatypeBean.getCode());
                    fieldsBean.setDataType(datatypeBean.getApply().getMYSQL().getType());
                    if (datatypeBean.getCode().contains("Date")) {
                        fieldsBean.setRemark("");
                    } else {
                        List<String> sizes = Lists.newArrayList(column.getSize() + "");
                        if (column.getDigits() != 0) {
                            sizes.add(column.getDigits() + "");
                        }
                        String size = String.join(",", sizes);
                        fieldsBean.setRemark("(" + size + ")");
                    }
                    fieldsBean.setPk(column.isPrimaryKey());
                    fieldsBean.setNotNull("NO".equals(column.getIsNullable()));
                    fieldsBean.setAutoIncrement("YES".equals(column.getIsAutoincrement()));
                    fieldsBean.setRelationNoShow(false);
                    fieldsBean.setDefaultValue(column.getDef());
                    fieldsBean.setUiHint("");
                    return fieldsBean;
                })
                .collect(Collectors.toList());
    }

    public static DatatypeBean findDatatypeBean(String mysqlTypeName, List<DatatypeBean> datatypeBeans) {
        List<DatatypeBean> list = datatypeBeans.stream()
                .filter(datatypeBean -> datatypeBean.getApply().getMYSQL().getType().equals(mysqlTypeName))
                .collect(Collectors.toList());
        if (list.size() == 0) {
            log.error(mysqlTypeName);
            throw new RuntimeException(mysqlTypeName + "没有对应的datatype");
        }
        return list.get(0);
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

    @SneakyThrows
    public static Table getTableInfo(String tableName, DatabaseMetaData databaseMetaData) {
        List<Table> tables;
        try (ResultSet rs = databaseMetaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            tables = Lists.newArrayList();
            while (rs.next()) {
                String viewName = rs.getString("TABLE_NAME");
                String remarks = rs.getString("REMARKS");
                tables.add(new Table(viewName, StringUtils.defaultString(remarks)));
            }
        }
        if (tables.isEmpty()) {
            return null;
        }
        return tables.get(0);
    }

    @SneakyThrows
    public static List<IndexsBean> getTableIndex(String tableName, DatabaseMetaData databaseMetaData) {
        List<IndexsBean> indexsBeans;
        try (ResultSet rs = databaseMetaData.getIndexInfo(null, null, tableName, false, false)) {
            indexsBeans = Lists.newArrayList();
            MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                String indexName = rs.getString("INDEX_NAME");
                IndexsBean indexsBean = new IndexsBean();
                indexsBean.setName(indexName);
                indexsBean.setIsUnique(!nonUnique);
                multiValueMap.add(indexName, columnName);
                indexsBean.setFields(multiValueMap.get(indexName));
                indexsBeans.add(indexsBean);
            }
            indexsBeans = indexsBeans.stream().distinct().collect(Collectors.toList());
        }
        return indexsBeans;
    }

    @SneakyThrows
    public static List<Column> getTableColumns(String tableName, DatabaseMetaData databaseMetaData) {
        String primaryColumnName;
        try (ResultSet result = databaseMetaData.getPrimaryKeys(null, null, tableName)) {
            primaryColumnName = "";
            if (result.next()) {
                primaryColumnName = result.getString("COLUMN_NAME");
            }
        }
        List<Column> columns;
        try (ResultSet rs = databaseMetaData.getColumns(null, null, tableName, null)) {
            columns = Lists.newArrayList();
            while (rs.next()) {
                Column column = new Column();
                String columnName = rs.getString("COLUMN_NAME");
                if (primaryColumnName.equals(columnName)) {
                    column.setPrimaryKey(true);
                }
                column.setName(columnName);
                int dataType = rs.getInt("DATA_TYPE");
                column.setType(dataType);
                String dataTypeName = rs.getString("TYPE_NAME");
                column.setTypeName(dataTypeName);
                int columnSize = rs.getInt("COLUMN_SIZE");
                column.setSize(columnSize);
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                column.setDigits(decimalDigits);
                int numPrecRadix = rs.getInt("NUM_PREC_RADIX");
                column.setPrecRadix(numPrecRadix);
                int nullAble = rs.getInt("NULLABLE");
                column.setNullable(nullAble);
                String remarks = rs.getString("REMARKS");
                column.setRemarks(StringUtils.defaultString(remarks));
                String columnDef = rs.getString("COLUMN_DEF");
                column.setDef(columnDef);
                int charOctetLength = rs.getInt("CHAR_OCTET_LENGTH");
                column.setCharOctetLength(charOctetLength);
                int ordinalPosition = rs.getInt("ORDINAL_POSITION");
                column.setOrdinalPosition(ordinalPosition);
                String isNullAble = rs.getString("IS_NULLABLE");
                column.setIsNullable(isNullAble);
                String isAutoincrement = rs.getString("IS_AUTOINCREMENT");
                column.setIsAutoincrement(isAutoincrement);
                columns.add(column);
            }
        }
        return columns;
    }

}
