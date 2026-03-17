
package com.gcs.handler;

import com.gcs.enums.MessageStatus;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MessageStatus 枚举类型处理器
 */
@MappedTypes(MessageStatus.class)
public class MessageStatusTypeHandler extends BaseTypeHandler<MessageStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, MessageStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public MessageStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return MessageStatus.valueOf(code);
    }

    @Override
    public MessageStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return MessageStatus.valueOf(code);
    }

    @Override
    public MessageStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return MessageStatus.valueOf(code);
    }
}
