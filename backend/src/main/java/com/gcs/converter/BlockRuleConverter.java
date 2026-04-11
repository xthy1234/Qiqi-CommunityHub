package com.gcs.converter;


import com.gcs.dto.BlockRuleCreateDTO;
import com.gcs.entity.BlockRule;
import com.gcs.vo.BlockRuleVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BlockRuleConverter {
    BlockRuleConverter INSTANCE = Mappers.getMapper(BlockRuleConverter.class);

    @Mapping(target = "userId", ignore = true) // userId 由后端从 Session 获取
    @Mapping(target = "enabled", constant = "true")
    BlockRule toEntity(BlockRuleCreateDTO dto);

    @Named("toVO")
    BlockRuleVO toVO(BlockRule entity);

    @IterableMapping(qualifiedByName = "toVO")
    List<BlockRuleVO> toVOList(List<BlockRule> entities);
}
