package com.original.flowcraft.web.controllers;

import com.original.flowcraft.core.TestSuiteRunner;
import com.original.flowcraft.entities.TestSuite;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/flowcraft/api")
public class DriverController {

    @PostMapping("/driver")
    public String driver(@RequestBody TestSuite testSuite) {
        TestSuiteRunner.runTestSuite(testSuite);
        return "test";
    }
}
