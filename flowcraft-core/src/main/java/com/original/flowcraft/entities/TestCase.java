package com.original.flowcraft.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TestCase {

    @JsonIgnore
    private String id;
    @JsonIgnore
    private String pid;

    private String name;

    private String className;

    private String fullClassName;

    private long timeElapsed;

    private boolean success;

    private String failureMessage;

    private String failureDetails;

    private List<Element> elements;

    private Map<String, String> arguments;

    private List<TestCase> testCases;

}
