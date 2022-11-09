package org.conffuzz;

import edu.neu.ccs.prl.meringue.CampaignValues;
import edu.neu.ccs.prl.meringue.FileUtil;
import edu.neu.ccs.prl.meringue.JvmLauncher;
import edu.neu.ccs.prl.meringue.SystemPropertyUtil;
import edu.neu.ccs.prl.pomelo.JvmConfiguration;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.conffuzz.internal.FuzzForkMain;
import org.conffuzz.internal.agent.ConfFuzzAgent;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ConfigurationMojoHelper {
    private static final boolean DEBUG = Boolean.getBoolean("conffuzz.debug");

    private ConfigurationMojoHelper() {
        throw new AssertionError();
    }

    static Properties getSystemPropertiesFromSurefire(MavenProject project) {
        Properties systemProperties = new Properties();
        for (Plugin p : project.getBuildPlugins()) {
            if (p.getArtifactId().contains("maven-surefire-plugin")) {
                Xpp3Dom configuration = (Xpp3Dom) p.getConfiguration();
                for (Xpp3Dom child : configuration.getChild("systemPropertyVariables").getChildren()) {
                    systemProperties.put(child.getName(), child.getValue());
                }
            }
        }
        return systemProperties;
    }

    static List<String> getJavaOptions(CampaignValues values, List<String> javaOptions, MavenProject project,
                                       File temporaryDirectory, File constraintFile) throws MojoExecutionException {
        try {
            List<String> options = new ArrayList<>(javaOptions);
            File systemPropertiesFile = new File(temporaryDirectory, "conffuzz.properties");
            SystemPropertyUtil.store(systemPropertiesFile, "", getSystemPropertiesFromSurefire(project));
            options.add(String.format("-D%s=%s", FuzzForkMain.PROPERTIES_KEY, systemPropertiesFile.getAbsolutePath()));
            options.add("-Djqf.failOnDeclaredExceptions");
            options.add("-DconfigFuzz=true");
            options.add("-Dconstraint.file=" + constraintFile.getAbsolutePath());
            options.add("-Dclass=" + values.getTestClassName());
            options.add("-Dmethod=" + values.getTestMethodName());
            File agentJar = FileUtil.getClassPathElement(ConfFuzzAgent.class);
            options.add(String.format("-javaagent:%s=%s,%s", agentJar.getAbsolutePath(), values.getTestClassName(),
                                      values.getTestMethodName()));
            if (DEBUG) {
                options.add(JvmLauncher.DEBUG_OPT + "5005");
            }
            return options;
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write system properties to file", e);
        }
    }

    static Map<String, String> getEnvironment(MavenProject project) {
        Map<String, String> environment = new HashMap<>();
        for (Plugin p : project.getBuildPlugins()) {
            if (p.getArtifactId().contains("maven-surefire-plugin")) {
                Xpp3Dom configuration = (Xpp3Dom) p.getConfiguration();
                for (Xpp3Dom child : configuration.getChild("environmentVariables").getChildren()) {
                    environment.put(child.getName(), child.getValue());
                }
            }
        }
        return JvmConfiguration.createEnvironment(environment, new String[0]);
    }
}
