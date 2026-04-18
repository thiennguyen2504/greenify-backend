package com.webdev.greenify.greenaction.mapper;

import com.webdev.greenify.file.entity.EventImageEntity;
import com.webdev.greenify.file.enumeration.EventImageType;
import com.webdev.greenify.file.mapper.ImageMapper;
import com.webdev.greenify.greenaction.dto.request.EventRequestDTO;
import com.webdev.greenify.greenaction.dto.response.EventImageResponseDTO;
import com.webdev.greenify.greenaction.dto.response.EventResponseDTO;
import com.webdev.greenify.greenaction.entity.EventEntity;
import com.webdev.greenify.station.mapper.AddressMapper;
import com.webdev.greenify.user.mapper.NGOProfileMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, ImageMapper.class, NGOProfileMapper.class})
public interface EventMapper {

    @Mapping(target = "organizer", ignore = true)
    EventEntity toEntity(EventRequestDTO dto);

    @Mapping(target = "organizer", source = "organizer.ngoProfile")
    @Mapping(target = "thumbnail", source = "images", qualifiedByName = "mapThumbnailField")
    @Mapping(target = "images", source = "images", qualifiedByName = "mapDetailImages")
    EventResponseDTO toResponse(EventEntity entity);

    @Mapping(target = "organizer", ignore = true)
    void updateEvent(EventRequestDTO dto, @MappingTarget EventEntity entity);

    EventImageResponseDTO toImageResponse(EventImageEntity entity);

    @Named("mapThumbnailField")
    default EventImageResponseDTO mapThumbnailField(List<EventImageEntity> images) {
        if (images == null) return null;
        return images.stream()
                .filter(img -> img.getImageType() == EventImageType.THUMBNAIL)
                .findFirst()
                .map(this::toImageResponse)
                .orElse(null);
    }

    @Named("mapDetailImages")
    default List<EventImageResponseDTO> mapDetailImages(List<EventImageEntity> images) {
        if (images == null) return null;
        return images.stream()
                .filter(img -> img.getImageType() != EventImageType.THUMBNAIL)
                .map(this::toImageResponse)
                .collect(Collectors.toList());
    }
}
