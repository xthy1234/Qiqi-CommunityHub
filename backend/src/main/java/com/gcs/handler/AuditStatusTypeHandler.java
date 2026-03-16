package com.gcs.handler;

import com.gcs.enums.AuditStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 审核状态 TypeHandler
 */
@MappedTypes(AuditStatus.class)
public class AuditStatusTypeHandler extends BaseTypeHandler<AuditStatus> {
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AuditStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }
    
    @Override
    public AuditStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Integer code = rs.getInt(columnName);
        return code == null ? null : AuditStatus.valueOf(code);
    }
    
    @Override
    public AuditStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Integer code = rs.getInt(columnIndex);
        return code == null ? null : AuditStatus.valueOf(code);
    }
    
    @Override
    public AuditStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Integer code = cs.getInt(columnIndex);
        return code == null ? null : AuditStatus.valueOf(code);
    }
}
