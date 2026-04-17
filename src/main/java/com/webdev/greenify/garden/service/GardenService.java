package com.webdev.greenify.garden.service;

import com.webdev.greenify.garden.dto.request.CreateSeedRequest;
import com.webdev.greenify.garden.dto.request.SelectSeedRequest;
import com.webdev.greenify.garden.dto.response.GardenArchiveResponse;
import com.webdev.greenify.garden.dto.response.PlantDailyLogResponse;
import com.webdev.greenify.garden.dto.response.PlantProgressResponse;
import com.webdev.greenify.garden.dto.response.SeedResponse;
import com.webdev.greenify.greenaction.dto.response.PagedResponse;

import java.time.LocalDate;
import java.util.List;

public interface GardenService {

    PagedResponse<SeedResponse> getAvailableSeeds(int page, int size);

    PlantProgressResponse selectSeed(SelectSeedRequest request);

    PlantProgressResponse getCurrentPlantProgress();

    List<PlantDailyLogResponse> getCurrentUserDailyLogs(LocalDate fromDate, LocalDate toDate);

    PagedResponse<GardenArchiveResponse> getGardenArchives(int page, int size);

    void updatePlantProgress(String userId, LocalDate actionDate, String greenPostUrl);

    SeedResponse createSeed(CreateSeedRequest request);
}
