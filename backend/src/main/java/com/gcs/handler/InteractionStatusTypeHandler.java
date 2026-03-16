package com.gcs.handler;

import com.gcs.enums.InteractionStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 互动状态 TypeHandler
 */
@MappedTypes(InteractionStatus.class)
public class InteractionStatusTypeHandler extends BaseTypeHandler<InteractionStatus> {
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, InteractionStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }
    
    @Override
    public InteractionStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Integer code = rs.getInt(columnName);
        return code == null ? null : InteractionStatus.valueOf(code);
    }
    
    @Override
    public InteractionStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Integer code = rs.getInt(columnIndex);
        return code == null ? null : InteractionStatus.valueOf(code);
    }
    
    @Override
    public InteractionStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Integer code = cs.getInt(columnIndex);
        return code == null ? null : InteractionStatus.valueOf(code);
    }
}
