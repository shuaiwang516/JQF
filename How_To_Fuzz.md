## Fuzz
```
mvn11 jqf:fuzz -Dclass=org.apache.hadoop.conf.TestDebug -Dmethod=test -Dconstraint.file=mappingDir/constraint -Djqf.failOnDeclaredExceptions -DsetSurefireConfig -DconfigFuzz
```

Flags:

Must have:
- -Dclass
- -Dmethod
- -Dconstraint.file
- -Djqf.failOnDeclaredExceptions
- -DsetSurefireConfig
- -DconfigFuzz

Optional:
- -Dgenerator.debug
- -Dtarget   (change the output directory)

## Reproduce
```
mvn11 jqf:repro -Dclass=org.apache.hadoop.conf.TestDebug  -Dmethod=test  -Dconstraint.file=mappingDir/constraint -Djqf.failOnDeclaredExceptions -DsetSurefireConfig -DconfigFuzz -DnotPrintConfig -Dinput=target/fuzz-results/org.apache.hadoop.conf.TestDebug/test/failures/id_000000
```

Flags:

Must have:
  - -Dclass
  - -Dmethod
  - -Dconstraint.file
  - -Djqf.failOnDeclaredExceptions
  - -DsetSurefireConfig
  - -DconfigFuzz

Optional:
- -DnotPrintConfig (Do not print the changed configuration information)
