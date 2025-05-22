package dev.snowdrop.internal.crd;

public class PackageStatus {
    private String phase;
    private String message;

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "PackageStatus{" +
            "phase='" + phase + '\'' +
            ", message='" + message + '\'' +
            '}';
    }
}
