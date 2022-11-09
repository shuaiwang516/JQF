# Configuration Fuzzing

## Fuzzing a configuration test

Fuzz a configuration using the fuzz goal:

```
mvn conffuzz:fuzz
-Dmeringue.testClass=<C> 
-Dmeringue.testMethod=<M>
-DconstraintFile=<N>
[-Dmeringue.duration=<D>]
[-Dmeringue.outputDirectory=<O>]
[-Dmeringue.javaOptions=<P>]
[-Dconffuzz.debug]
```

Where:

* \<C\> is the fully-qualified name of the test class.
* \<M\> is the name of the test method.
* \<N\> is the path of the configuration constraint file.
* \<D\> is the maximum amount of time to execute the fuzzing campaign for specified in the ISO-8601 duration format (
  e.g., 2 days, 3 hours, and 4 minutes is "P2DT3H4M"). The default value is one day.
* \<O\> is the path of the directory to which the output files should be written.
  The default value is ${project.build.directory}/meringue.
* \<P\> is a list of Java command line options that should be used for test JVMs.
* The presence of -Dconffuzz.debug indicates that campaign JVMs should suspend and wait for a debugger to attach
  on port 5005. By default, campaign JVMs do not suspend and wait for a debugger to attach.

## Analyzing a configuration fuzzing campaign

Fuzz a analyze configuration fuzzing campaign using the analyze goal:

```
mvn conffuzz:analyze
-Dmeringue.testClass=<C> 
-Dmeringue.testMethod=<M>
-DconstraintFile=<N>
[-Dmeringue.duration=<D>]
[-Dmeringue.outputDirectory=<O>]
[-Dmeringue.javaOptions=<P>]
[-Dmeringue.maxTraceSize=<Z>]
[-Dmeringue.debug]
[-Dmeringue.timeout=<Y>]
[-Dmeringue.jacocoFormats=<F>]
```

Where:

* \<C\> is the fully-qualified name of the test class.
* \<M\> is the name of the test method.
* \<N\> is the path of the configuration constraint file.
* \<D\> is the maximum amount of time to execute the fuzzing campaign for specified in the ISO-8601 duration format (
  e.g., 2 days, 3 hours, and 4 minutes is "P2DT3H4M"). The default value is one day.
* \<O\> is the path of the directory to which the output files should be written.
  The default value is ${project.build.directory}/meringue.
* \<P\> is a list of Java command line options that should be used for test JVMs.
* \<Z\> is the maximum number of frames to include in stack traces taken for failures. By default, a maximum of 5 frames
  are included.
* The presence of -Dmeringue.debug indicates that forked analysis JVMs should suspend and wait for a debugger to attach
  on port 5005. By default, forked analysis JVMs do not suspend and wait for a debugger to attach.
* \<Y\> is the maximum amount of time in seconds to execute a single input during analysis or -1 if no timeout should be
  used. By default, a timeout value of 600 seconds is used.
* \<F\> is a list of JaCoCo report formats to be generated. The formats XML, HTML, CSV are supported. By default, all
  formats are generated.
