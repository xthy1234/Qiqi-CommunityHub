package com.gcs.vo;

import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 圈子成员信息 VO
 */
@Data
@Schema(description = "圈子成员信息视图对象")
public class CircleMemberVO {
    
    @Schema(description = "成员关系 ID", example = "1")
    private Long id;
    
    @Schema(description = "用户信息")
    private UserSimpleVO user;
    
    @Schema(description = "角色 (0:成员，1:管理员，2:圈主)", example = "0")
    private Integer role;
    
    @Schema(description = "角色描述", example = "成员")
    private String roleDescription;
    
    @Schema(description = "加入时间", example = "2026-03-17 12:00:00")
    private LocalDateTime joinTime;
    
    @Schema(description = "状态 (1:正常，0:已退出)", example = "1")
    private CommonStatus status;

}
