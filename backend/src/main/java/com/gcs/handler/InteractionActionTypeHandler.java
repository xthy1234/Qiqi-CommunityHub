package com.gcs.handler;

import com.gcs.enums.InteractionActionType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 互动操作类型转换器
 */
@MappedTypes(InteractionActionType.class)
public class InteractionActionTypeHandler extends BaseTypeHandler<InteractionActionType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, InteractionActionType parameter, JdbcType jdbcType) throws SQLException {
        if (parameter != null) {
            ps.setInt(i, parameter.getCode());
        }
    }

    @Override
    public InteractionActionType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return InteractionActionType.valueOf(code);
    }

    @Override
    public InteractionActionType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return InteractionActionType.valueOf(code);
    }

    @Override
    public InteractionActionType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return InteractionActionType.valueOf(code);
    }
}
