package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 版本对比结果 VO
 */
@Data
@Schema(description = "版本对比结果")
public class VersionCompareResultVO {

    @Schema(description = "源版本信息")
    private VersionInfoVO sourceVersion;

    @Schema(description = "目标版本信息")
    private VersionInfoVO targetVersion;

    /**
     * 版本信息内部类
     */
    @Data
    @Schema(description = "版本信息")
    public static class VersionInfoVO {
        
        @Schema(description = "版本号", example = "1")
        private Integer version;

        @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
        private LocalDateTime createTime;

        @Schema(description = "操作人 ID", example = "1")
        private Long operatorId;

        @Schema(description = "修改摘要", example = "更新了标题")
        private String changeSummary;

        @Schema(description = "文章内容（JSON）")
        private Map<String, Object> content;
    }
}
