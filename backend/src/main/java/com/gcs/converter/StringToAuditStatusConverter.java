package com.gcs.converter;

import com.gcs.enums.AuditStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * String 转 AuditStatus 转换器
 */
@Component
public class StringToAuditStatusConverter implements Converter<String, AuditStatus> {

    @Override
    public AuditStatus convert(String source) {
        if (source == null || source.trim().isEmpty()) {
            return null;
        }

        try {
            Integer code = Integer.parseInt(source.trim());
            return AuditStatus.valueOf(code);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的审核状态值：" + source);
        }
    }
}
