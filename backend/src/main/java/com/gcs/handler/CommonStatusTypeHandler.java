package com.gcs.handler;

import com.gcs.enums.CommonStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 通用状态 TypeHandler
 */
@MappedTypes(CommonStatus.class)
public class CommonStatusTypeHandler extends BaseTypeHandler<CommonStatus> {
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, CommonStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }
    
    @Override
    public CommonStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Integer code = rs.getInt(columnName);
        return code == null ? null : CommonStatus.valueOf(code);
    }
    
    @Override
    public CommonStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Integer code = rs.getInt(columnIndex);
        return code == null ? null : CommonStatus.valueOf(code);
    }
    
    @Override
    public CommonStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Integer code = cs.getInt(columnIndex);
        return code == null ? null : CommonStatus.valueOf(code);
    }
}
