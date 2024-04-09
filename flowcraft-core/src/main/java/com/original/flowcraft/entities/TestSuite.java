package com.original.flowcraft.entities;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TestSuite {

    private String name;

    private List<TestCase> testCases;

    private long timeElapsed;

    private DriverOptions driverOptions;

    private Map<String, String> arguments;
}
