package org.conffuzz;

import edu.neu.ccs.prl.meringue.FuzzingMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.conffuzz.internal.ConfFuzzFramework;

import java.io.File;
import java.util.List;
import java.util.Map;

@Mojo(name = "fuzz", defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class ConfigurationFuzzingMojo extends FuzzingMojo {
    /**
     * Make the required super class "framework" parameter optional; it is not used.
     */
    @Parameter(readonly = true)
    @SuppressWarnings("unused")
    private String framework;
    @Parameter(property = "constraintFile", required = true)
    private File constraintFile;

    @Override
    public String getFrameworkClassName() {
        return ConfFuzzFramework.class.getName();
    }

    @Override
    public List<String> getJavaOptions() throws MojoExecutionException {
        return ConfigurationMojoHelper.getJavaOptions(this, super.getJavaOptions(), getProject(),
                                                      getTemporaryDirectory(), constraintFile);
    }

    @Override
    public Map<String, String> getEnvironment() throws MojoExecutionException {
        return ConfigurationMojoHelper.getEnvironment(getProject());
    }
}
