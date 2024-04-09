package com.original.flowcraft.entities;

import lombok.*;

@Data
@Builder(builderClassName = "Builder", setterPrefix = "with")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Element {
    private String by;
    private String locator;
    private String event;
    private String value;
    private boolean disable;
    private String control;
    private String lable;
}
