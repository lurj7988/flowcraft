package com.original.flowcraft.entities;

import lombok.Data;

import java.util.List;

@Data
public class DriverOptions {

    private String browserName;

    private String remoteAddress;

    private List<String> arguments;
}
