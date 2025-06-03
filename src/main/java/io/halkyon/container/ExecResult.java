package io.halkyon.container;

/**
 * Class to hold results from a "podman exec" command
 */
public class ExecResult {
    int exitCode;

    String stdout;

    String stderr;

    public ExecResult(int exitCode, String string, String string1) {
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }
}