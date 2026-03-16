package com.gcs.handler;

import com.gcs.enums.CategoryStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 分类状态 TypeHandler
 */
@MappedTypes(CategoryStatus.class)
public class CategoryStatusTypeHandler extends BaseTypeHandler<CategoryStatus> {
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, CategoryStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }
    
    @Override
    public CategoryStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Integer code = rs.getInt(columnName);
        if (rs.wasNull()) {
            return null;
        }
        return CategoryStatus.valueOf(code);
    }
    
    @Override
    public CategoryStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Integer code = rs.getInt(columnIndex);
        if (rs.wasNull()) {
            return null;
        }
        return CategoryStatus.valueOf(code);
    }
    
    @Override
    public CategoryStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Integer code = cs.getInt(columnIndex);
        if (cs.wasNull()) {
            return null;
        }
        return CategoryStatus.valueOf(code);
    }
}
