package com.original.flowcraft.entities;

import lombok.Data;
import org.openqa.selenium.WebDriver;

/**
 * TestContext
 * <p>
 * TestContext is a class that holds the context of the test.
 * It is a placeholder for now.
 * It can be extended to hold more information.
 * It can be used to pass the context between different modules.
 * </p>
 *
 * @author naulu
 * @since 1.0
 */
@Data
public class TestContext {

    private WebDriver driver;

    private TestSuite testSuite;

}
