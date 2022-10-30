/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.plugin;

import java.io.*;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.*;

import edu.berkeley.cs.jqf.fuzz.configfuzz.ConfigTracker;
import edu.berkeley.cs.jqf.fuzz.configfuzz.DefConfCollectionGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.fuzz.repro.ReproGuidance;
import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.runner.Result;

/**
 * Maven plugin for replaying a test case produced by JQF.
 *
 * @author Rohan Padhye
 */
@Mojo(name="repro",
        requiresDependencyResolution=ResolutionScope.TEST)
public class ReproGoal extends AbstractMojo {

    @Parameter(defaultValue="${project}", required=true, readonly=true)
    MavenProject project;

    @Parameter(defaultValue="${project.build.directory}", readonly=true)
    private File target;

    /**
     * The fully-qualified name of the test class containing methods
     * to fuzz.
     *
     * <p>This class will be loaded using the Maven project's test
     * classpath. It must be annotated with {@code @RunWith(JQF.class)}</p>
     */
    @Parameter(property="class", required=true)
    private String testClassName;

    /**
     * The name of the method to fuzz.
     *
     * <p>This method must be annotated with {@code @Fuzz}, and take
     * one or more arguments (with optional junit-quickcheck
     * annotations) whose values will be fuzzed by JQF.</p>
     *
     * <p>If more than one method of this name exists in the
     * test class or if the method is not declared
     * {@code public void}, then the fuzzer will not launch.</p>
     */
    @Parameter(property="method", required=true)
    private String testMethod;

    /**
     * Input file or directory to reproduce test case(s).
     *
     * <p>These files will typically be taken from the test corpus
     * ("queue") directory or the failures ("crashes") directory
     * generated by JQF in a previous fuzzing run, for the same
     * test class and method.</p>
     *
     */
    @Parameter(property="input", required=true)
    private String input;

    /**
     * Output file to dump coverage info.
     *
     * <p>This is an optional parameter. If set, the value is the name
     * of a file where JQF will dump code coverage information for
     * the test inputs being replayed.</p>
     */
    @Parameter(property="logCoverage")
    private String logCoverage;

    /**
     * Comma-separated list of FQN prefixes to exclude from
     * coverage instrumentation.
     *
     * <p>This property is only useful if {@link #logCoverage} is
     * set. The semantics are similar to the similarly named
     * property in the goal <code>jqf:fuzz</code>.</p>
     */
    @Parameter(property="excludes")
    private String excludes;

    /**
     * Comma-separated list of FQN prefixes to forcibly include,
     * even if they match an exclude.
     *
     * <p>Typically, these will be a longer prefix than a prefix
     * in the excludes clauses.</p>
     *
     * <p>This property is only useful if {@link #logCoverage} is
     * set. The semantics are similar to the similarly named
     * property in the goal <code>jqf:fuzz</code>.</p>
     */
    @Parameter(property="includes")
    private String includes;

    /**
     * Whether to print the args to each test case.
     *
     * <p>The input file being repro'd is usually a sequence of bytes
     * that is decoded by the junit-quickcheck generators corresponding
     * to the parameters declared in the test method. Unless the test method
     * contains just one arg of type InputStream, the input file itself
     * does not directly correspond to the args sent to the test method.</p>
     *
     * <p>If this flag is set, then the args decoded from a repro'd input
     * file are first printed to standard output before invoking the test
     * method.</p>
     */
    @Parameter(property="printArgs")
    private boolean printArgs;

    /**
     * Whether to dump the args to each test case to file(s).
     *
     * <p>The input file being repro'd is usually a sequence of bytes
     * that is decoded by the junit-quickcheck generators corresponding
     * to the parameters declared in the test method. Unless the test method
     * contains just one arg of type InputStream, the input file itself
     * does not directly correspond to the args sent to the test method.</p>
     *
     * <p>If provided, then the args decoded from a repro'd input
     * file are dumped to corresponding files
     * in this directory before invoking the test method.</p>
     */
    @Parameter(property="dumpArgsDir")
    private String dumpArgsDir;

    @Parameter(property="setSurefireConfig")
    private boolean setMavenSurefireConfiguration;

    /**
     *  Whether to run JQF with configuration fuzzing
     *
     *  <p>If this property is set to true, there is a non-fuzzed pre round
     *  to collect the exercised configuration parameter set for the under
     *  fuzzing test
     *  </p>
     *
     *  <p>If not provided, defaults to {@code false}.</p>
     */
    @Parameter(property="configFuzz")
    private boolean configurationFuzzing;

    @Parameter(property="printConfig")
    private boolean printConfig;

    // For configuration fuzzing project -- a flag to check whether to print out the current
    // changed configuration or not.
    private String isReproGoal = "repro.info";

    /** Store parent configuration that pass and current configuration that fail; TreeMap to make sure looping is
     * deterministic */
    private Map<String, String> defaultConfig = new TreeMap<>();
    private Map<String, String> parentConfig = new TreeMap<>();
    private Map<String, String> failedConfig = new TreeMap<>();

    public static void setEnv(Map<String, String> envMap, Log log) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                String envName = entry.getKey();
                String envValue = entry.getValue();
                log.debug("Setting environment variable [" + envName + "] [" + envValue + "]");
                writableEnv.put(envName, envValue);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }

    public void setConfigurationFromMavenSurefire(Log log) {
        for(Plugin p : project.getBuildPlugins()) {
            if (p.getArtifactId().contains("maven-surefire-plugin")) {
                Xpp3Dom environmentVariables = ((Xpp3Dom)p.getConfiguration()).getChild("environmentVariables");
                Xpp3Dom systemPropertyVariables = ((Xpp3Dom)p.getConfiguration()).getChild("systemPropertyVariables");
                Map<String, String> envMap = new HashMap<>();
                for (int i = 0; i < environmentVariables.getChildCount(); i++) {
                    Xpp3Dom child = environmentVariables.getChild(i);
                    String envName = child.getName();
                    String envValue = child.getValue();
                    envMap.put(envName, envValue);
                }
                setEnv(envMap, log);
                for (int i = 0; i < systemPropertyVariables.getChildCount(); i++) {
                    Xpp3Dom child = systemPropertyVariables.getChild(i);
                    String systemPropertyName = child.getName();
                    String systemPropertyValue = child.getValue();
                    log.debug("Setting system property [" + systemPropertyName + "] [" + systemPropertyValue + "]");
                    System.setProperty(systemPropertyName, systemPropertyValue);
                }
            }
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        System.setProperty(isReproGoal, "true");
        ClassLoader loader;
        ReproGuidance guidance;
        DefConfCollectionGuidance preRoundGuidance;
        Log log = getLog();
        PrintStream out = System.out; // TODO: Re-route to logger from super.getLog()
        Result result;

        // Set Maven Surefire Configuration
        if (setMavenSurefireConfiguration) {
            out.println("Set Maven-Surefire-Plugin Configuration");
            setConfigurationFromMavenSurefire(log);
        }
        // Configure classes to instrument
        if (excludes != null) {
            System.setProperty("janala.excludes", excludes);
        }
        if (includes != null) {
            System.setProperty("janala.includes", includes);
        }

        try {
            List<String> classpathElements = project.getTestClasspathElements();

            loader = new InstrumentingClassLoader(
                    classpathElements.toArray(new String[0]),
                    getClass().getClassLoader());
        } catch (DependencyResolutionRequiredException|MalformedURLException e) {
            throw new MojoExecutionException("Could not get project classpath", e);
        }

        // If a coverage dump file was provided, enable logging via system property
        if (logCoverage != null) {
            System.setProperty("jqf.repro.logUniqueBranches", "true");
        }

        // If args should be printed, set system property
        if (printArgs) {
            System.setProperty("jqf.repro.printArgs", "true");
        }

        // If args should be dumped, set system property
        if (dumpArgsDir != null) {
            System.setProperty("jqf.repro.dumpArgsDir", dumpArgsDir);
        }

        File inputFile = new File(input);

        if (!inputFile.exists() || !inputFile.canRead()) {
            throw new MojoExecutionException("Cannot find or open file " + input);
        }

        // Pre round to get default configuration set and parent round to get the parent configuration value set
        if (configurationFuzzing) {
            File parentFile = new File(input.replace("id_", "parent_"));
            if (!parentFile.exists() || !parentFile.canRead()) {
                throw new MojoExecutionException("Cannot find or open file " + parentFile);
            }

            // Pre round for test to get default configuration
            preRoundGuidance = new DefConfCollectionGuidance(out);
            Result pre_result;
            Result parent_result;
            try {
                out.println("==================================Pre-Round==================================");
                pre_result = GuidedFuzzing.run(testClassName, testMethod, loader, preRoundGuidance, out);
                defaultConfig.putAll(ConfigTracker.getConfigMap());
                if (printConfig) {
                    printMap(defaultConfig, out);
                }
                log.debug("[JQF] Num of fuzzed config parameter = " + ConfigTracker.getMapSize());
                System.out.println("[JQF] Num of fuzzed config parameter = " + ConfigTracker.getMapSize());

                // Parent round to get parent configuration change
                out.println("==================================Parent-Round==================================");
                guidance = new ReproGuidance(parentFile, null);
                parent_result = GuidedFuzzing.run(testClassName, testMethod, loader, guidance, out);
                parentConfig.putAll(ConfigTracker.getConfigMap());
                if (printConfig) {
                    printMap(parentConfig, out);
                }
            } catch (ClassNotFoundException e) {
                throw new MojoExecutionException("Could not load test class", e);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException("Bad request", e);
            } catch (RuntimeException e) {
                throw new MojoExecutionException("Internal error", e);
            } catch (IOException e) {
                throw new MojoExecutionException("Pre round error", e);
            }
            if (!pre_result.wasSuccessful()) {
                throw new MojoFailureException("Pre Round for Configuration Fuzzing is not successful");
            }
            if (!parent_result.wasSuccessful()) {
                printDiffConfig(defaultConfig, parentConfig, out);
		out.println("Parent Round for Configuration Fuzzing is not successful");
		//throw new MojoFailureException("Parent Round for Configuration Fuzzing is not successful");
            }
        }

        try {
            guidance = new ReproGuidance(inputFile, null);
            out.println("==================================Failure-Round==================================");
            result = GuidedFuzzing.run(testClassName, testMethod, loader, guidance, out);
            if (configurationFuzzing) {
                failedConfig.putAll(ConfigTracker.getConfigMap());
                if (printConfig) {
                    printMap(parentConfig, out);
                }
                printDiffConfig(parentConfig, failedConfig, out);

                File configFile = new File(input.replace("id_", "config_"));
                if (!configFile.exists() || !configFile.canRead()) {
                    throw new MojoExecutionException("Cannot find or open file " + configFile);
                }
                checkConfigSame(getConfigFromFile(configFile),failedConfig);
            }

        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Could not load test class", e);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Bad request", e);
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("File not found", e);
        } catch (IOException e) {
            throw new MojoExecutionException("I/O error", e);
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Internal error", e);
        }

        // If a coverage dump file was provided, then dump coverage
        if (logCoverage != null) {
            Set<String> coverageSet = guidance.getBranchesCovered();
            assert (coverageSet != null); // Should not happen if we set the system property above
            SortedSet<String> sortedCoverage = new TreeSet<>(coverageSet);
            try (PrintWriter covOut = new PrintWriter(new File(logCoverage))) {
                for (String b : sortedCoverage) {
                    covOut.println(b);
                }
            } catch (IOException e) {
                log.error("Could not dump coverage info.", e);
            }
        }

        if (!result.wasSuccessful()) {
            throw new MojoFailureException("Test case produces a failure.");
        }
    }

    private void checkConfigSame(Map<String, String> failure, Map<String, String> repro) throws MojoExecutionException {
        if (failure == null || repro == null) {
            throw new MojoExecutionException("[Generator-Non-Deterministic] Configuration Map is null");
        }
        if (!repro.keySet().equals(failure.keySet())) {
            throw new MojoExecutionException("[Generator-Non-Deterministic] Two Rounds have different Generated Set");
        }
        for (Map.Entry<String, String> entry : repro.entrySet()) {
            String failedKey = entry.getKey();
            String failedValue = entry.getValue();
            String parentValue = failure.get(failedKey);
            if (!Objects.equals(failedValue, parentValue)) {
                throw new MojoExecutionException("[Generator-Non-Deterministic] Two Rounds have " +
                        "different Generated value on " + failedKey + " = " + failedKey + " vs " + parentValue);
            }
        }
    }

    private Map<String, String> getConfigFromFile(File configFile) throws IOException {
        Map<String, String> conf = new TreeMap<>();
        BufferedReader br = new BufferedReader(new FileReader(configFile));
        String line;
        while ((line = br.readLine())!= null) {
            String [] pair = line.split(ZestGuidance.configSeparator);
            if (pair.length != 2) {
                throw new IOException("Unable to split configuration parameter and value: " + line);
            }
            String key = pair[0];
            String value = pair[1];
            conf.put(key.trim(), value.trim());
        }
        return conf;
    }

    private void printDiffConfig(Map<String, String> parent, Map<String, String> failed, PrintStream out) {
        out.println("[TEST]=" + testClassName + "#" + testMethod);
        for (Map.Entry<String, String> entry : failed.entrySet()) {
            String failedKey = entry.getKey();
            String failedValue = entry.getValue();
            if (parent.containsKey(failedKey)) {
                String parentValue = parent.get(failedKey);
                if (anyNull(failedValue, parentValue)) {
                    continue;
                }
                else if (Objects.equals(failedValue, parentValue)) {
                    out.println("[PARENT-CONFIG-SAME] " + failedKey + " = " + parentValue);
                } else {
                    out.println("[PARENT-CONFIG-DIFF] " + failedKey + " = " + parentValue + " -> " + failedValue);
                }
            } else {
                out.println("[PARENT-CONFIG-NEW] " + failedKey + " -> " + failedValue);
            }
        }
    }

    private boolean anyNull(String str1, String str2) {
        return Objects.equals(str1, str2) && (str1 == null || str1.equals("null") || str1.equals(""));
    }

    private void printMap(Map<String, String> map, PrintStream out) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            out.println("[CONFIG-PRINT] " + entry.getKey() + " = " + entry.getValue());
        }
    }
}
