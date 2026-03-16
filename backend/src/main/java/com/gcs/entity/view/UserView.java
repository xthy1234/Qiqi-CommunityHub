package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.User;
import com.gcs.enums.CommonStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class UserView extends User implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：性别描述
     */
    private String genderDescription;

    /**
     * 扩展字段：状态描述
     */
    private String statusDescription;

    /**
     * 扩展字段：文章数量
     */
    private Integer articleCount;

    /**
     * 扩展字段：粉丝数量
     */
    private Integer followerCount;

    /**
     * 扩展字段：关注数量
     */
    private Integer followingCount;

    /**
     * 构造函数
     * @param user 原始用户对象
     */
    public UserView(User user) {
        if (user != null) {
            // 手动复制属性，避免使用过时的BeanUtils
            this.setId(user.getId());
            this.setAccount(user.getAccount());
            this.setPassword(user.getPassword());
            this.setNickname(user.getNickname());
            this.setAvatar(user.getAvatar());
            this.setGender(user.getGender());
            this.setPhone(user.getPhone());
            this.setEmail(user.getEmail());
            this.setBirthday(user.getBirthday());
            this.setSignature(user.getSignature());
            this.setStatus(user.getStatus());
            this.setCreateTime(user.getCreateTime());
            this.setUpdateTime(user.getUpdateTime());
            this.setLastLoginTime(user.getLastLoginTime());
            this.setLastLoginIp(user.getLastLoginIp());
            
            // 设置扩展字段
            this.genderDescription = getGenderDescription(user.getGender());
            this.statusDescription = getStatusDescription(user.getStatus());
        }
    }

    /**
     * 获取性别描述
     */
    private String getGenderDescription(Integer gender) {
        if (gender == null) return "保密";
        switch (gender) {
            case 0: return "女";
            case 1: return "男";
            default: return "保密";
        }
    }

    /**
     * 获取状态描述
     */
    private String getStatusDescription(CommonStatus status) {
        if (status == null) return "未知";
        return status == CommonStatus.ENABLED ? "启用" : "禁用";
    }
}
