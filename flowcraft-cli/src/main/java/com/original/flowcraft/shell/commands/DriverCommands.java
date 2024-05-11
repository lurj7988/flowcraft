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
import com.original.flowcraft.utils.ClassPathUtils;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.component.flow.ComponentFlow;
import org.springframework.shell.component.flow.ResultMode;
import org.springframework.shell.standard.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@ShellComponent
@Slf4j
public class DriverCommands {

    private final ComponentFlow.Builder componentFlowBuilder;

    public DriverCommands(ComponentFlow.Builder componentFlowBuilder) {
        this.componentFlowBuilder = componentFlowBuilder;
    }

    @ShellMethod("driver")
    public String driver(@ShellOption(value = {"-p", "--path"}, help = "Path to the file to be processed") String path,
                         @ShellOption(value = {"-b", "--browser"}, help = "borwser name(chrome,edge,firefox)") String browserName,
                         @ShellOption(value = {"-r", "--remote"}, help = "selenium hub address") String remoteAddress) {
        log.info("driver path: {},browserName: {},remoteAddress: {}", path, browserName, remoteAddress);
        TestSuiteRunner.runTestSuite(getTestSuiteByExcel(path, browserName, remoteAddress));
        return "driver" + path;
    }

    @ShellMethod("test2")
    public String test2() {
        log.info(ClassPathUtils.getDeployWarPath());
        return ClassPathUtils.getDeployWarPath();
    }

    @ShellMethod("test")
    public String test(@ShellOption(value = {"-p", "--path"}, defaultValue = ShellOption.NULL, help = "config path") String path,
                       @ShellOption(value = {"-d", "--driver"}, defaultValue = ShellOption.NULL, help = "web driver type", valueProvider = DriverValuesProvider.class) String driver,
                       @ShellOption(value = {"-r", "--remote"}, defaultValue = ShellOption.NULL, help = "selenium hub address") String remote) {
        Map<String, String> driverSelectItems = new HashMap<>();
        driverSelectItems.put(DriverManagerType.CHROME.getBrowserName(), DriverManagerType.CHROME.getBrowserName().toLowerCase());
        driverSelectItems.put(DriverManagerType.EDGE.getBrowserName(), DriverManagerType.EDGE.getBrowserName().toLowerCase());
        driverSelectItems.put(DriverManagerType.FIREFOX.getBrowserName(), DriverManagerType.FIREFOX.getBrowserName().toLowerCase());
        ComponentFlow flow = componentFlowBuilder.clone().reset()
                .withPathInput("path")
                .name("Path")
                .resultValue(path)
                .resultMode(ResultMode.ACCEPT)
                .and()
                .withSingleItemSelector("driver")
                .name("Driver")
                .defaultSelect(DriverManagerType.CHROME.getBrowserName())
                .selectItems(driverSelectItems)
                .resultValue(driver)
                .resultMode(ResultMode.ACCEPT)
                .and()
                .withStringInput("remote")
                .name("Remote")
                .resultValue(remote)
                .resultMode(ResultMode.ACCEPT)
                .and().build();
        ComponentFlow.ComponentFlowResult result = flow.run();
        Map<String, String> context = new HashMap<>();
        result.getContext().stream().forEach(e -> {
            if (e.getValue() != null) {
                context.put(e.getKey().toString(), e.getValue().toString());
            } else {
                context.put(e.getKey().toString(), null);
            }
        });
        path = context.get("path");
        driver = context.get("driver");
        remote = context.get("remote");
        log.info("path: {},driver: {},remote: {}", path, driver, remote);
        if (path.contains("json")) {
            TestSuiteRunner.runTestSuite(getTestSuiteByJson(path, driver, remote));
        } else if (path.contains("xlsx")) {
            TestSuiteRunner.runTestSuite(getTestSuiteByExcel(path, driver, remote));
        } else {
            return "sorry,you doesn't input a json or xlsx file.";
        }
        return "success";

    }

    @Bean
    DriverCommands.DriverValuesProvider driverValuesProvider() {
        return new DriverCommands.DriverValuesProvider();
    }

    public static class DriverValuesProvider implements ValueProvider {

        private final static String[] VALUES = new String[]{
                DriverManagerType.CHROME.getBrowserName().toLowerCase(),
                DriverManagerType.EDGE.getBrowserName().toLowerCase(),
                DriverManagerType.FIREFOX.getBrowserName().toLowerCase()
        };

        @Override
        public List<CompletionProposal> complete(CompletionContext completionContext) {
            return Arrays.stream(VALUES).map(CompletionProposal::new).collect(Collectors.toList());
        }
    }

    private TestSuite getTestSuiteByJson(String path, String browserName, String remoteAddress) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            TestSuite testSuite = objectMapper.readValue(Paths.get(path).toFile(), TestSuite.class);
            DriverOptions driverOptions = testSuite.getDriverOptions();
            //如果参数中有browserName则覆盖配置文件中的browserName
            Optional.ofNullable(browserName).ifPresent(driverOptions::setBrowserName);
            //如果参数中有remoteAddress则覆盖配置文件中的remoteAddress
            Optional.ofNullable(remoteAddress).ifPresent(driverOptions::setRemoteAddress);
            return testSuite;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private TestSuite getTestSuiteByExcel(String path, String browserName, String remoteAddress) {
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
        //如果参数中有browserName则覆盖配置文件中的browserName
        Optional.ofNullable(browserName).ifPresent(driverOptions::setBrowserName);
        //如果参数中有remoteAddress则覆盖配置文件中的remoteAddress
        Optional.ofNullable(remoteAddress).ifPresent(driverOptions::setRemoteAddress);
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
