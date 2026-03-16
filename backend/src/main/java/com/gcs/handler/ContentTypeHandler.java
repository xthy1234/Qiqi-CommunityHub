package com.gcs.handler;

import com.gcs.enums.ContentType;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 内容类型转换器
 */
@MappedTypes(ContentType.class)
public class ContentTypeHandler extends BaseTypeHandler<ContentType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ContentType parameter, JdbcType jdbcType) throws SQLException {
        if (parameter != null) {
            ps.setString(i, parameter.getCode());
        }
    }

    @Override
    public ContentType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return ContentType.valueOfCode(code);
    }

    @Override
    public ContentType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return ContentType.valueOfCode(code);
    }

    @Override
    public ContentType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return ContentType.valueOfCode(code);
    }
}
