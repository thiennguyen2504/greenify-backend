package com.webdev.greenify.garden.enumeration;

public enum PlantCycleType {
    EASY(30, 45),
    MEDIUM(50, 80),
    HARD(90, 150);

    private final int minDaysToMature;
    private final int maxDaysToMature;

    PlantCycleType(int minDaysToMature, int maxDaysToMature) {
        this.minDaysToMature = minDaysToMature;
        this.maxDaysToMature = maxDaysToMature;
    }

    public int getMinDaysToMature() {
        return minDaysToMature;
    }

    public int getMaxDaysToMature() {
        return maxDaysToMature;
    }
}