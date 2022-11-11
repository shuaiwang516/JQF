package org.conffuzz.internal.agent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that instrumentation was applied to a class.
 */
@Retention(RetentionPolicy.CLASS)
public @interface ConfFuzzInstrumented {
}
