package com.original.flowcraft.shell.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.original.flowcraft.core.TestSuiteRunner;
import com.original.flowcraft.entities.DriverOptions;
import com.original.flowcraft.entities.Element;
import com.original.flowcraft.entities.TestCase;
import com.original.flowcraft.entities.TestSuite;
import com.original.flowcraft.shell.utils.DefaultExcelRowHandler;
import com.original.flowcraft.shell.utils.ExcelParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.*;
import java.util.stream.Collectors;

@ShellComponent
@Slf4j
public class DriverCommand {

    @ShellMethod("driver")
    public String driver(@ShellOption(value = {"-p", "--path"}, help = "Path to the file to be processed") String path) {
        log.info(path);
        TestSuiteRunner.runTestSuite(getTestSuiteByExcel(path));
        return "driver" + path;
    }


    private TestSuite getTestSuiteByExcel(String path) {
        Map<String, List<Map<String, Object>>> excel = new ExcelParser()
                .readExcel(path, new DefaultExcelRowHandler());
        List<Map<String, Object>> test_suite = excel.get("test_suite");
        List<Map<String, Object>> test_case = excel.get("test_case");
        List<Map<String, Object>> web_elements = excel.get("web_elements");
        List<Map<String, Object>> suite_arguments = excel.get("suite_arguments");
        List<Map<String, Object>> case_arguments = excel.get("case_arguments");
        List<Map<String, Object>> webdriver_config = excel.get("webdriver_config");
        TestSuite testSuite = new TestSuite();
        testSuite.setName(test_suite.get(0).get("name").toString());
        suite_arguments = suite_arguments.stream()
                .filter(p -> p.get("suite_id").equals(test_suite.get(0).get("id"))).toList();
        Map<String, String> arguments = new HashMap<>();
        suite_arguments.forEach(p -> arguments.put(p.get("key").toString(), p.get("value").toString()));
        testSuite.setArguments(arguments);
        DriverOptions driverOptions = new DriverOptions();
        ObjectMapper objectMapper = new ObjectMapper();
        webdriver_config.forEach(p -> {
            driverOptions.setBrowserName(p.get("browser_name").toString());
            driverOptions.setRemoteAddress(p.get("remote_address").toString());
            try {
                driverOptions.setArguments(objectMapper.readValue(p.get("arguments").toString(), new TypeReference<>() {
                }));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        testSuite.setDriverOptions(driverOptions);

        test_case = test_case.stream().filter(p -> p.get("suite_id").equals(test_suite.get(0).get("id"))).toList();
        List<TestCase> testCases = test_case.stream().map(p -> {
            TestCase testCase = new TestCase();
            testCase.setId(p.get("id").toString());
            testCase.setPid(p.get("pid").toString());
            testCase.setName(p.get("name").toString());
            testCase.setClassName(p.get("class_name").toString());
            testCase.setFullClassName(p.get("full_class_name").toString());
            Map<String, String> args = new HashMap<>();
            case_arguments.stream().filter(q -> q.get("case_id").equals(p.get("id"))).forEach(q -> {
                args.put(q.get("key").toString(), q.get("value").toString());
            });
            testCase.setArguments(args);
            List<Element> elements = new ArrayList<>();
            web_elements.stream().filter(q -> q.get("case_id").equals(p.get("id"))).forEach(q -> {
                Element element = Element.builder()
                        .withControl(String.valueOf(q.get("control")))
                        .withLable(String.valueOf(q.get("lable")))
                        .withBy(String.valueOf(q.get("by")))
                        .withLocator(String.valueOf(q.get("locator")))
                        .withEvent(String.valueOf(Optional.ofNullable(q.get("event")).orElse("sendKeys")))
                        .withValue(String.valueOf(q.get("value")))
                        .withDisable(Boolean.parseBoolean(String.valueOf(q.get("disable")))).build();
                elements.add(element);
            });
            testCase.setElements(elements);
            testCase.setTestCases(new ArrayList<>());
            return testCase;
        }).toList();

        testCases = makeTree(testCases, "");
        testSuite.setTestCases(testCases);

        return testSuite;
    }

    private List<TestCase> makeTree(List<TestCase> routeItems, String pid) {
        // 子类
        List<TestCase> children = routeItems.stream().filter(x -> pid.equals(x.getPid()))
                .collect(Collectors.toList());
        // 后辈中的非子类
        List<TestCase> successor = routeItems.stream().filter(x -> !pid.equals(x.getPid()))
                .collect(Collectors.toList());
        children.forEach(x -> {
            makeTree(successor, x.getId()).forEach(y -> x.getTestCases().add(y));
        });
        return children;
    }
}
