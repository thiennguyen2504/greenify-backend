package com.webdev.greenify.user.dto;

import com.webdev.greenify.user.enumeration.NGOProfileStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NGOProfileFilterRequestDTO {
    String orgName;
    NGOProfileStatus status;
    String search;
    
    @Builder.Default
    int page = 0;
    @Builder.Default
    int size = 10;
}
