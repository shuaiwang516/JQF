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

    @Parameter(property="notPrintConfig")
    private boolean notPrintConfig;

    // For configuration fuzzing project -- a flag to check whether to print out the current
    // changed configuration or not.
    private String isReproGoal = "repro.info";

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
        File configFile = new File(input.replace("id", "config"));
        if (!inputFile.exists() || !inputFile.canRead()) {
            throw new MojoExecutionException("Cannot find or open file " + input);
        }
        if (!configFile.exists() || !configFile.canRead()) {
            throw new MojoExecutionException("Cannot find or open file " + configFile);
        }

        // Pre round to get default configuration set
        if (configurationFuzzing) {
            // Pre round for test to get default configuration
            preRoundGuidance = new DefConfCollectionGuidance(out);
            try {
                result = GuidedFuzzing.run(testClassName, testMethod, loader, preRoundGuidance, out);
                log.debug("After preRound mapping size = " + ConfigTracker.getMapSize());
                System.out.println("[JQF] After preRound mapping size = " + ConfigTracker.getMapSize());
                printChangedConfig(configFile, out);
            } catch (ClassNotFoundException e) {
                throw new MojoExecutionException("Could not load test class", e);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException("Bad request", e);
            } catch (RuntimeException e) {
                throw new MojoExecutionException("Internal error", e);
            } catch (IOException e) {
                throw new MojoExecutionException("Pre round error", e);
            }
            if (!result.wasSuccessful()) {
                throw new MojoFailureException("Pre Round for Configuration Fuzzing is not successful");
            }
        }

        try {
            guidance = new ReproGuidance(inputFile, null);
            result = GuidedFuzzing.run(testClassName, testMethod, loader, guidance, out);
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

    private void printChangedConfig(File configFile, PrintStream out) throws IOException {
        if (!configurationFuzzing || notPrintConfig) {
            return;
        }
        Map<String, String> configMap = ConfigTracker.getConfigMap();
        if (configMap == null || configMap.size() == 0) {
            throw new RuntimeException("No default configuration tracked - Please " +
                    "check pre-round is executed correctly");
        }

        BufferedReader br = new BufferedReader(new FileReader(configFile));
        String line;
        while ((line = br.readLine())!= null) {
            String [] pair = line.split(ZestGuidance.configSeparator);
            String key = null;
            String value = null;
            if (pair.length == 1) {
                key = pair[0];
                value = "null";
            } else if (pair.length == 2) {
                key = pair[0];
                value = pair[1];
            }
            else if (pair.length > 2 || key == null || value == null) {
                throw new IOException("Unable to split configuration parameter and value: " + line);
            }
            if (configMap.containsKey(key)) {
                String defaultValue = configMap.get(key);
                if (!Objects.equals(value, defaultValue)) {
                    out.println("[CONFIG-CHANGE] " + key + " = " + defaultValue + " -> " + value);
                }
            } else {
                out.println("[CONFIG-CHANGE] " + key + " = no-default -> " + value);
            }
        }
    }
}
