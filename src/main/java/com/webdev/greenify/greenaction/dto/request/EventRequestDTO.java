package com.webdev.greenify.greenaction.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webdev.greenify.file.dto.ImageRequestDTO;
import com.webdev.greenify.greenaction.enumeration.GreenEventStatus;
import com.webdev.greenify.greenaction.enumeration.GreenEventType;
import com.webdev.greenify.station.dto.AddressRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDTO {

    @NotBlank(message = "Tiêu đề là bắt buộc")
    private String title;

    private String description;

    @NotNull(message = "Loại sự kiện là bắt buộc")
    private GreenEventType eventType;

    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    @Future(message = "Thời gian bắt đầu phải ở tương lai")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    @Future(message = "Thời gian kết thúc phải ở tương lai")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @Min(value = 1, message = "Số lượng người tham gia tối đa phải ít nhất là 1")
    private Long maxParticipants;

    @Min(value = 1, message = "Số lượng người tham gia tối đa phải ít nhất là 1")
    private Long minParticipants;

    @Positive(message = "Số giờ hạn hủy phải lớn hơn 0")
    private Long cancelDeadlineHoursBefore;

    @Positive(message = "Số giờ hạn đăng ký phải lớn hơn 0")
    private Long signUpDeadlineHoursBefore;

    @Positive(message = "Số giờ nhắc trước phải lớn hơn 0")
    private Long reminderHoursBefore;

    @Positive(message = "Số giờ gửi cảm ơn sau sự kiện phải lớn hơn 0")
    private Long thankYouHoursAfter;

    @NotNull(message = "Điểm thưởng là bắt buộc")
    @Positive(message = "Điểm thưởng phải lớn hơn 0")
    private Double rewardPoints;

    private GreenEventStatus status;

    @Valid
    @NotNull(message = "Ảnh đại diện là bắt buộc")
    private ImageRequestDTO thumbnail;

    @Valid
    private List<ImageRequestDTO> images;

    @NotNull(message = "Địa chỉ là bắt buộc")
    @Valid
    private AddressRequestDTO address;

    private String participationConditions;
}
