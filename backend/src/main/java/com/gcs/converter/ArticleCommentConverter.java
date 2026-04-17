package com.gcs.converter;

import com.gcs.entity.Comment;
import com.gcs.entity.User;
import com.gcs.vo.ArticleCommentVO;
import com.gcs.vo.UserSimpleVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

/**
 * ArticleComment 对象转换器
 */
@Mapper(componentModel = "spring")
public interface ArticleCommentConverter {
    
    ArticleCommentConverter INSTANCE = Mappers.getMapper(ArticleCommentConverter.class);
    
    /**
     * 将 Comment 转换为 ArticleCommentVO（需要额外传入用户信息和子评论）
     * 由于转换逻辑复杂，建议使用 Service 层的手动转换方法
     */
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "replyToUser", ignore = true)
    @Mapping(target = "topReplies", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    @Mapping(target = "isEdited", ignore = true)
    ArticleCommentVO toVO(Comment comment);
    
    List<ArticleCommentVO> toVOList(List<Comment> comments);
}
