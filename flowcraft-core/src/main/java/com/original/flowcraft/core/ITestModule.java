package com.original.flowcraft.core;

import com.original.flowcraft.entities.TestCase;
import com.original.flowcraft.entities.TestContext;

public interface ITestModule {

    TestCase getTestCase();

    void beforeTest(TestCase testCase, TestContext context);

    void test() throws Exception;

    void afterTest();
}
