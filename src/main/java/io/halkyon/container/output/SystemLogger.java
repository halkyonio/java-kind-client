package io.halkyon.container.output;

public class SystemLogger implements Logger {

    private final boolean ansiColorEnabled;

    public SystemLogger() {
        this(true);
    }

    public SystemLogger(boolean ansiColorEnabled) {
        this.ansiColorEnabled = ansiColorEnabled;
    }

    @Override
    public void stdout(String message) {
        System.out.println(prepare(message));
    }

    @Override
    public void stderr(String message) {
        System.err.println(prepare(message));
    }

    public boolean isAnsiColorEnabled() {
        return this.ansiColorEnabled;
    }
}