package com.gcs.converter;

import com.gcs.dto.CommentCreateDTO;
import com.gcs.dto.CommentUpdateDTO;
import com.gcs.dto.CommentDTO;
import com.gcs.entity.Comment;
import com.gcs.vo.CommentVO;
import com.gcs.vo.CommentDetailVO;
import com.gcs.vo.CommentTreeVO;
import com.gcs.vo.CommentWithUserVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Comment 对象转换器
 */
@Mapper(componentModel = "spring")
public interface CommentConverter {
    
    CommentConverter INSTANCE = Mappers.getMapper(CommentConverter.class);
    
    // DTO to Entity
    Comment toEntity(CommentDTO dto);
    
    Comment toEntity(CommentCreateDTO dto);
    
    Comment toEntity(CommentUpdateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    CommentVO toVO(Comment entity);
    
    @Named("toDetailVO")
    CommentDetailVO toDetailVO(Comment entity);
    
    @Named("toTreeVO")
    CommentTreeVO toTreeVO(Comment entity);
    
    @Named("toCommentWithUserVO")
    CommentWithUserVO toCommentWithUserVO(Comment entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<CommentVO> toVOList(List<Comment> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<CommentDetailVO> toDetailVOList(List<Comment> entities);
    
    @IterableMapping(qualifiedByName = "toTreeVO")
    List<CommentTreeVO> toTreeVOList(List<Comment> entities);
    
    @IterableMapping(qualifiedByName = "toCommentWithUserVO")
    List<CommentWithUserVO> toCommentWithUserVOList(List<Comment> entities);
    
    // Update existing entity
    void updateEntity(CommentUpdateDTO dto, @MappingTarget Comment entity);
}
