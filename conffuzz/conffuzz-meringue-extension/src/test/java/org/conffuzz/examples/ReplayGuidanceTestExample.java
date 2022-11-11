package org.conffuzz.examples;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.configfuzz.ConfigTracker;
import org.junit.runner.RunWith;

import java.util.Map;


@RunWith(JQF.class)
@SuppressWarnings("NewClassNamingConvention")
public class ReplayGuidanceTestExample {
    @Fuzz
    public void test(@From(TestConfigurationGenerator.class) Map<String, String> configuration) {
        System.out.println(configuration);
        String value1 = configuration.getOrDefault("red", "none");
        ConfigTracker.track("red", value1);
        if (!value1.equals("none")) {
            String value2 = configuration.getOrDefault("blue", "none");
            ConfigTracker.track("blue", value2);
            if (!value2.equals("none")) {
                String value3 = configuration.getOrDefault("green", "none");
                ConfigTracker.track("green", value3);
                if (!value3.equals("none")) {
                    throw new IllegalArgumentException();
                }
            }
        }
    }
}
