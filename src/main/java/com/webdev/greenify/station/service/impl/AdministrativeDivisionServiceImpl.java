package com.webdev.greenify.station.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.webdev.greenify.common.exception.AppException;
import com.webdev.greenify.station.dto.ProvinceResponseDTO;
import com.webdev.greenify.station.dto.WardResponseDTO;
import com.webdev.greenify.station.service.AdministrativeDivisionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Service
public class AdministrativeDivisionServiceImpl implements AdministrativeDivisionService {

    private static final String PROVINCES_API_BASE_URL = "https://provinces.open-api.vn/api/v2";
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 5000;

    private final RestTemplate restTemplate;

    public AdministrativeDivisionServiceImpl() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public List<ProvinceResponseDTO> getProvinces() {
        URI uri = UriComponentsBuilder.fromUriString(PROVINCES_API_BASE_URL + "/p/")
                .build(true)
                .toUri();

        try {
            ProvinceApiResponse[] response = restTemplate.getForObject(uri, ProvinceApiResponse[].class);
            if (response == null) {
                return List.of();
            }

            return Arrays.stream(response)
                    .map(this::toProvinceResponseDTO)
                    .toList();
        } catch (RestClientException ex) {
            throw new AppException("Không thể lấy danh sách tỉnh/thành", HttpStatus.BAD_GATEWAY);
        }
    }

    @Override
    public List<WardResponseDTO> getWardsByProvinceCode(int provinceCode) {
        URI uri = UriComponentsBuilder.fromUriString(PROVINCES_API_BASE_URL + "/w/")
                .queryParam("province", provinceCode)
                .build(true)
                .toUri();

        try {
            WardApiResponse[] response = restTemplate.getForObject(uri, WardApiResponse[].class);
            if (response == null) {
                return List.of();
            }

            return Arrays.stream(response)
                    .map(this::toWardResponseDTO)
                    .toList();
        } catch (RestClientException ex) {
            throw new AppException("Không thể lấy danh sách xã/phường theo tỉnh", HttpStatus.BAD_GATEWAY);
        }
    }

    private ProvinceResponseDTO toProvinceResponseDTO(ProvinceApiResponse response) {
        return ProvinceResponseDTO.builder()
                .code(response.code())
                .codename(response.codename())
                .divisionType(response.divisionType())
                .name(response.name())
                .phoneCode(response.phoneCode())
                .build();
    }

    private WardResponseDTO toWardResponseDTO(WardApiResponse response) {
        return WardResponseDTO.builder()
                .code(response.code())
                .codename(response.codename())
                .divisionType(response.divisionType())
                .name(response.name())
                .provinceCode(response.provinceCode())
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProvinceApiResponse(
            Integer code,
            String codename,
            @JsonProperty("division_type") String divisionType,
            String name,
            @JsonProperty("phone_code") Integer phoneCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WardApiResponse(
            Integer code,
            String codename,
            @JsonProperty("division_type") String divisionType,
            String name,
            @JsonProperty("province_code") Integer provinceCode) {
    }
}