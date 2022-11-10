package org.conffuzz.examples;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SuppressWarnings("NewClassNamingConvention")
public class RunWithExample {
    @Test
    public void test() {
        ExampleUtil.values.add("t");
    }
}