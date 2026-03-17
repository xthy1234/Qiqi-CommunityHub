package com.gcs.dto;

import com.gcs.enums.CommonStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 圈子详情 VO
 */
@Data
@Schema(description = "圈子详情视图对象")
public class CircleDetailVO {
    
    @Schema(description = "圈子 ID", example = "1")
    private Long id;
    
    @Schema(description = "圈子名称", example = "游戏战队")
    private String name;
    
    @Schema(description = "圈子描述", example = "欢迎加入我们的游戏战队")
    private String description;
    
    @Schema(description = "圈子头像 URL", example = "http://example.com/avatar.jpg")
    private String avatar;
    
    @Schema(description = "圈主 ID", example = "1")
    private Long ownerId;
    
    @Schema(description = "圈主昵称", example = "张三")
    private String ownerNickname;
    
    @Schema(description = "圈主头像", example = "http://example.com/owner.jpg")
    private String ownerAvatar;
    
    @Schema(description = "圈子类型 (0:私密，1:公开，2:个人空间)", example = "1")
    private Integer type;
    
    @Schema(description = "圈子状态 (1:正常，0:解散)", example = "1")
    private CommonStatus status;
    
    @Schema(description = "成员数量", example = "50")
    private Integer memberCount;
    
    @Schema(description = "当前用户角色 (1:圈主，2:管理员，3:成员，null:非成员)", example = "3")
    private Integer currentUserRole;
    
    @Schema(description = "是否已加入", example = "true")
    private Boolean isJoined;
    
    @Schema(description = "创建时间", example = "2026-03-17 12:00:00")
    private String createTime;
    
    @Schema(description = "更新时间", example = "2026-03-17 12:00:00")
    private String updateTime;
}
