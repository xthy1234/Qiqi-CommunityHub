package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.Report;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 举报信息视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:09
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("report")
public class ReportView extends Report implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：审核状态描述
     */
    private String reviewStatusDescription;

    /**
     * 扩展字段：举报人昵称
     */
    private String reporterNickname;

    /**
     * 扩展字段：被举报人昵称
     */
    private String reportedUserNickname;

    /**
     * 构造函数
     * @param report 原始举报对象
     */
    public ReportView(Report report) {
        if (report != null) {
            // 手动复制属性，避免使用过时的BeanUtils
            this.setId(report.getId());
            this.setContentTitle(report.getContentTitle());
            this.setContentCategory(report.getContentCategory());
            this.setReportedUserAccount(report.getReportedUserAccount());
            this.setReportedNickName(report.getReportedNickName());
            this.setReportReason(report.getReportReason());
            this.setReportTime(report.getReportTime());
            this.setReporterAccount(report.getReporterAccount());
            this.setReporterId(report.getReporterId());
            this.setReplyContent(report.getReplyContent());
            this.setReviewStatus(report.getReviewStatus());
            this.setReviewerAccount(report.getReviewerAccount());
            this.setReviewTime(report.getReviewTime());
            this.setStatus(report.getStatus());
            this.setCreateTime(report.getCreateTime());
            this.setUpdateTime(report.getUpdateTime());
        }
    }
}
