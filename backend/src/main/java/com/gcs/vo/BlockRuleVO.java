package com.gcs.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "屏蔽规则视图")
public class BlockRuleVO implements Serializable {
    private Long id;
    private String ruleType;
    private String ruleValue;
    private Boolean enabled;
    
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
