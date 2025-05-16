package dev.snowdrop.container.output;

public class LogConfig {

    private String DEFAULT_LOG_LEVEL = "info";
    private Boolean DEFAULT_USE_TIMESTAMPS = Boolean.TRUE;

    private String logLevel;
    private Boolean useTimestamps;
    private Logger logger;


    public LogConfig(
        String logLevel,
        Boolean useTimestamps,
        Logger logger
    ) {
        this.logLevel = logLevel != null ? logLevel : DEFAULT_LOG_LEVEL;
        this.useTimestamps = useTimestamps != null ? useTimestamps : DEFAULT_USE_TIMESTAMPS;
        this.logger = logger != null ? logger : new SystemLogger();
    }

    public String getLogLevel() {
        return logLevel;
    }

    public Logger getLogger() {
        return logger;
    }

    public Boolean getUseTimestamps() {
        return useTimestamps;
    }

}