package dev.snowdrop.container;

import java.util.Map;

public class ExecConfig {

    /**
     * The command to run.
     */
    private String[] command;

    /**
     * The user to run the exec process.
     */
    private String user;

    /**
     * Key-value pairs of environment variables.
     */
    private Map<String, String> envVars;

    /**
     * The working directory for the exec process.
     */
    private String workDir;


    public String[] getCommand() {
        return command;
    }

    public void setCommand(String[] command) {
        this.command = command;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Map<String, String> getEnvVars() {
        return envVars;
    }

    public void setEnvVars(Map<String, String> envVars) {
        this.envVars = envVars;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }
}