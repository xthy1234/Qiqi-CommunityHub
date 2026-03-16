package com.gcs.handler;

import com.gcs.enums.CommentStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 评论状态类型转换器
 */
@MappedTypes(CommentStatus.class)
public class CommentStatusTypeHandler extends BaseTypeHandler<CommentStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, CommentStatus parameter, JdbcType jdbcType) throws SQLException {
        if (parameter != null) {
            ps.setInt(i, parameter.getCode());
        }
    }

    @Override
    public CommentStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return CommentStatus.valueOf(code);
    }

    @Override
    public CommentStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return CommentStatus.valueOf(code);
    }

    @Override
    public CommentStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return CommentStatus.valueOf(code);
    }
}
