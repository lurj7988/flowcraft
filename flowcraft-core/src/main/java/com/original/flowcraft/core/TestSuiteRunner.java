package com.original.flowcraft.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.original.flowcraft.entities.DriverOptions;
import com.original.flowcraft.entities.TestCase;
import com.original.flowcraft.entities.TestContext;
import com.original.flowcraft.entities.TestSuite;
import com.original.flowcraft.utils.ApplicationUtils;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.github.bonigarcia.wdm.config.DriverManagerType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TestSuiteRunner {

    public static void runTestSuite(TestSuite testSuite) {
        DriverOptions driverOptions = testSuite.getDriverOptions();
        WebDriverManager wdm = WebDriverManager.getInstance(driverOptions.getBrowserName());
        wdm.avoidShutdownHook();
        Optional.ofNullable(driverOptions.getRemoteAddress()).ifPresent(wdm::remoteAddress);
        Capabilities capabilities = createCapabilities(driverOptions);
        Optional.ofNullable(capabilities).ifPresent(wdm::capabilities);
        TestContext context = new TestContext();
        WebDriver driver = wdm.create();
        driver.manage().window().maximize();
        context.setDriver(driver);
        context.setTestSuite(testSuite);
        List<ITestModule> testModules = testSuite.getTestCases().stream()
                .map(p -> convertTestCaseToTestModule(p, context)).collect(Collectors.toList());
        try {
            run(testModules, context);
        } catch (Exception e) {
            log.error("Error occurred while running test suite", e);
        } finally {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Path path = Paths.get("target", "screenshots");
                objectMapper.writeValue(path.resolve(testSuite.getName() + ".json").toFile(), testSuite);
            } catch (IOException e) {
                log.error("", e);
            }
            context.getDriver().quit();
        }
    }

    protected static Capabilities createCapabilities(DriverOptions driverOptions) {
        DriverManagerType driverManagerType = DriverManagerType.valueOf(driverOptions.getBrowserName().toUpperCase());
        if (driverOptions.getArguments() == null) {
            return null;
        }
        switch (driverManagerType) {
            case CHROME:
            case CHROMIUM:
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments(driverOptions.getArguments());
                return chromeOptions;
            case EDGE:
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments(driverOptions.getArguments());
                return edgeOptions;
            case FIREFOX:
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.addArguments(driverOptions.getArguments());
                return firefoxOptions;
            default:
                throw new IllegalArgumentException("Unsupported browser: " + driverOptions.getBrowserName());
        }
    }

    public static ITestModule convertTestCaseToTestModule(TestCase testCase, TestContext context) {
        Map<String, ITestModule> iTestModuleMap = ApplicationUtils.getBeansOfType(ITestModule.class);
        for (ITestModule iTestModule : iTestModuleMap.values()) {
            if (iTestModule.getClass().getName().equals(testCase.getFullClassName())) {
                iTestModule.beforeTest(testCase, context);
                return iTestModule;
            }
        }
        throw new RuntimeException("No such test module found: " + testCase.getFullClassName());
    }

    public static void run(List<ITestModule> testModules, TestContext context) throws Exception {
        for (ITestModule testModule : testModules) {
            TestCase testCase = testModule.getTestCase();
            if (testCase.isSuccess()) {
                continue;
            }
            Instant startTime = Instant.now();
            // 执行你的代码
            try {
                testModule.test();
                testCase.setSuccess(true);
                if (CollectionUtils.isNotEmpty(testCase.getTestCases())) {
                    List<ITestModule> children = testCase.getTestCases().stream()
                            .map(p -> convertTestCaseToTestModule(p, context)).collect(Collectors.toList());
                    run(children, context);
                }
            } catch (Exception e) {
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                testCase.setSuccess(false);
                testCase.setFailureMessage(e.getMessage());
                testCase.setFailureDetails(stringWriter.toString());
                // 截图
                if (context.getDriver() instanceof ChromeDriver) {
                    ChromeDriver driver = (ChromeDriver) context.getDriver();
                    File sourceFile = driver.getScreenshotAs(OutputType.FILE);
                    Path path = Paths.get("target", "screenshots");
                    try {
                        // FileCopyUtils.copy(sourceFile, path.resolve(testCase.getName() + ".png").toFile());
                        FileUtils.copyFile(sourceFile, path.resolve(testCase.getName() + ".png").toFile());
                    } catch (IOException ex) {
                        log.error("Error occurred while copying screenshot", ex);
                    }
                }
                // log.error("Error occurred while running test module", e);
                throw e;
            } finally {
                Instant endTime = Instant.now();
                Duration duration = Duration.between(startTime, endTime);
                testCase.setTimeElapsed(duration.toMillis());
                testModule.afterTest();
            }
        }
    }
}
