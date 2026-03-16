package com.gcs.handler;

import com.gcs.enums.FeedbackStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 反馈状态 TypeHandler
 */
@MappedTypes(FeedbackStatus.class)
public class FeedbackStatusTypeHandler extends BaseTypeHandler<FeedbackStatus> {
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, FeedbackStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }
    
    @Override
    public FeedbackStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Integer code = rs.getInt(columnName);
        return code == null ? null : FeedbackStatus.valueOf(code);
    }
    
    @Override
    public FeedbackStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Integer code = rs.getInt(columnIndex);
        return code == null ? null : FeedbackStatus.valueOf(code);
    }
    
    @Override
    public FeedbackStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Integer code = cs.getInt(columnIndex);
        return code == null ? null : FeedbackStatus.valueOf(code);
    }
}
