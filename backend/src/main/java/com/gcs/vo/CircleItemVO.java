package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 圈子列表项 VO
 */
@Data
@Schema(description = "圈子列表项视图对象")
public class CircleItemVO {
    
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
    
    @Schema(description = "圈子类型 (0:私密，1:公开，2:个人空间)", example = "1")
    private Integer type;
    
    @Schema(description = "成员数量", example = "50")
    private Integer memberCount;
    
    @Schema(description = "是否已加入", example = "true")
    private Boolean isJoined;
    
    @Schema(description = "未读消息数", example = "5")
    private Integer unreadCount;
}
