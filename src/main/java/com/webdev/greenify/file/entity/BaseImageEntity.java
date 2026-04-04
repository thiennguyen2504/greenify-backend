package com.webdev.greenify.file.entity;

import com.webdev.greenify.common.entity.BaseEntity;
import com.webdev.greenify.user.enumeration.ImageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@DiscriminatorColumn(name = "image_type", discriminatorType = DiscriminatorType.STRING)
public abstract class BaseImageEntity extends BaseEntity {
    @Column(nullable = false)
    private String bucketName;

    @Column(nullable = false)
    private String objectKey;

    @Column
    private String imageUrl;

    @Column
    private ImageStatus status;
}
