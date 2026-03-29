package com.webdev.greenify.user.dto;

import com.webdev.greenify.user.entity.RoleEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDetailResponseDTO {
    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private Set<RoleEntity> roles;
    private boolean enabled;
}
