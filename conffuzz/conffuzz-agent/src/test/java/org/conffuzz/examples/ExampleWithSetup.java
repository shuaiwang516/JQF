package org.conffuzz.examples;

import org.junit.*;

@SuppressWarnings("NewClassNamingConvention")
public class ExampleWithSetup {
    @Before
    public void before() {
        ExampleUtil.values.add("b");
    }

    @After
    public void after() {
        ExampleUtil.values.add("a");
    }

    @Test
    public void test() {
        ExampleUtil.values.add("t");
    }

    @BeforeClass
    public static void beforeClass() {
        ExampleUtil.values.add("bc");
    }

    @AfterClass
    public static void afterClass() {
        ExampleUtil.values.add("ac");
    }
}
