package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.CircleChat;
import com.gcs.vo.CircleChatSessionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 圈子聊天数据访问接口
 * @author
 * @date 2026-03-20
 */
public interface CircleChatDao extends BaseMapper<CircleChat> {

    /**
     * 分页查询圈子聊天记录
     * @param page 分页对象
     * @param circleId 圈子 ID
     * @return 聊天记录列表
     */
    List<CircleChat> selectChatHistory(IPage<CircleChat> page,
                                       @Param("circleId") Long circleId);

    /**
     * 查询圈子最新消息
     * @param circleId 圈子 ID
     * @return 最新消息
     */
    CircleChat selectLastMessage(@Param("circleId") Long circleId);

    /**
     * 根据消息 ID 查询消息（包含发送者信息）
     * @param messageId 消息 ID
     * @return 消息
     */
    CircleChat selectMessageById(@Param("messageId") Long messageId);

    /**
     * 查询用户加入的圈子的会话列表
     * @param userId 用户 ID
     * @return 会话列表
     */
    List<CircleChatSessionVO> selectConversations(@Param("userId") Long userId);

    /**
     * 统计圈子的未读消息数
     * @param userId 用户 ID
     * @param circleId 圈子 ID
     * @return 未读消息数
     */
    Integer countUnreadMessages(@Param("userId") Long userId,
                                @Param("circleId") Long circleId);
}
