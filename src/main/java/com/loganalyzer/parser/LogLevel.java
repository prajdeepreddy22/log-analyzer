package com.loganalyzer.parser;

public enum LogLevel {

    DEBUG, INFO, WARN, ERROR, FATAL, UNKNOWN;

    public static LogLevel fromString(String level) {
        if (level == null) return UNKNOWN;

        return switch (level.toUpperCase().trim()) {
            case "DEBUG" -> DEBUG;
            case "INFO" -> INFO;
            case "WARN", "WARNING" -> WARN;
            case "ERROR" -> ERROR;
            case "FATAL", "SEVERE" -> FATAL;
            default -> UNKNOWN;
        };
    }

    public int getSeverity() {
        return switch (this) {
            case FATAL -> 5;
            case ERROR -> 4;
            case WARN -> 3;
            case INFO -> 2;
            case DEBUG -> 1;
            case UNKNOWN -> 0;
        };
    }
}