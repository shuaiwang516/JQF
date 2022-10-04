```
mvn11 jqf:fuzz -Dclass=org.apache.hadoop.conf.TestDebug -Dmethod=test -Dconstraint.file=mappingDir/constraint -Djqf.failOnDeclaredExceptions -DsetSurefireConfig -DconfigFuzz
```

Flags:
- -Dclass
- -Dmethod
- -Dconstraint.file
- -Djqf.failOnDeclaredExceptions
- -DsetSurefireConfig
- -DconfigFuzz
- -Dgenerator.debug
