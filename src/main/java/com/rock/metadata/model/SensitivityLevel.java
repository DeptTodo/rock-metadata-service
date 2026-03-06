package com.rock.metadata.model;

public enum SensitivityLevel {
    /** Public data, no restrictions */
    PUBLIC,
    /** Internal use only */
    INTERNAL,
    /** Sensitive data, requires access control */
    SENSITIVE,
    /** Highly sensitive (PII, financial, medical), strict access control */
    HIGHLY_SENSITIVE
}
