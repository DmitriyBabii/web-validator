package com.validator.models.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TidyReport {
    private List<TidyMessageReport> messageReports = new ArrayList<>();
}
