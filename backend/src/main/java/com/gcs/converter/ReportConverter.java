package com.gcs.converter;

import com.gcs.dto.ReportCreateDTO;
import com.gcs.entity.Report;
import com.gcs.vo.ReportVO;
import com.gcs.vo.ReportDetailVO;
import com.gcs.vo.ReportStatisticsVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Report 对象转换器
 */
@Mapper(componentModel = "spring")
public interface ReportConverter {
    
    ReportConverter INSTANCE = Mappers.getMapper(ReportConverter.class);
    
    // DTO to Entity
    Report toEntity(ReportCreateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    ReportVO toVO(Report entity);
    
    @Named("toDetailVO")
    ReportDetailVO toDetailVO(Report entity);
    
    @Named("toStatisticsVO")
    ReportStatisticsVO toStatisticsVO(Report entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<ReportVO> toVOList(List<Report> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<ReportDetailVO> toDetailVOList(List<Report> entities);
    
    // Update existing entity (空方法，用于手动更新)
    void updateEntity(ReportCreateDTO dto, @MappingTarget Report entity);
}
