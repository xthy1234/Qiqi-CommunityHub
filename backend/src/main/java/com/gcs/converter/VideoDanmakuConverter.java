package com.gcs.converter;


import com.gcs.dto.DanmakuSendDTO;
import com.gcs.entity.VideoDanmaku;
import com.gcs.vo.VideoDanmakuVO;
import com.gcs.vo.VideoDanmakuDetailVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 视频弹幕转换器
 * @author 
 * @date 2026-04-05
 */
@Mapper(componentModel = "spring")
public interface VideoDanmakuConverter {

    VideoDanmakuConverter INSTANCE = Mappers.getMapper(VideoDanmakuConverter.class);

    // DTO to Entity
    VideoDanmaku toEntity(DanmakuSendDTO dto);

    // Entity to VO
    @Named("toVO")
    VideoDanmakuVO toVO(VideoDanmaku entity);

    @Named("toDetailVO")
    VideoDanmakuDetailVO toDetailVO(VideoDanmaku entity);

    // List conversion
    @IterableMapping(qualifiedByName = "toVO")
    List<VideoDanmakuVO> toVOList(List<VideoDanmaku> entities);

    @IterableMapping(qualifiedByName = "toDetailVO")
    List<VideoDanmakuDetailVO> toDetailVOList(List<VideoDanmaku> entities);
}
