package com.techleadsim.web.dto;
import jakarta.validation.constraints.NotNull;

public record OfferRequestDto(@NotNull Long personId) {}
