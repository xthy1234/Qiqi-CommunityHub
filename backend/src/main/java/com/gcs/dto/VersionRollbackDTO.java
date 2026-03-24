package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 版本回滚 DTO
 */
@Data
@Schema(description = "版本回滚数据")
public class VersionRollbackDTO {

    @NotNull(message = "版本号不能为空")
    @Min(value = 1, message = "版本号必须大于 0")
    @Schema(description = "目标版本号", example = "5", required = true)
    private Integer version;
}
