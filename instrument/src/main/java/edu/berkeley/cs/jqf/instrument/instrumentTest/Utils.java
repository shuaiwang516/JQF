package edu.berkeley.cs.jqf.instrument.instrumentTest;

import edu.berkeley.cs.jqf.instrument.log.Log;

import java.util.Objects;

public class Utils {
    private static String className = System.getProperty("class").replace(".", "/");
    private static String classMethod = System.getProperty("method");

    public static String getCurrentClass() {
        return className;
    }

    public static String getCurrentMethod() {
        return classMethod;
    }

    public static Boolean isTestClassTransformNeeded (String cName) {
        Log.d2f("ClassName = " + className + " comparedClassName = " + cName);
        if (Objects.equals(cName, className)) {
            return true;
        }
        return false;
    }

    public static Boolean isTestMethodTransformNeeded (String classMethod) {
        if (Objects.equals(className, getCurrentMethod())) {
            return true;
        }
        return false;
    }
}
