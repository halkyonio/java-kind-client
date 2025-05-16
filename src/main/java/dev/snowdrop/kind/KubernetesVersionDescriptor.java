package dev.snowdrop.kind;

import static java.lang.String.format;

public class KubernetesVersionDescriptor implements Comparable<KubernetesVersionDescriptor> {
    private final int major, minor, patch;

    KubernetesVersionDescriptor(final int major, final int minor, final int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    @Override
    public int compareTo(final KubernetesVersionDescriptor o) {
        if (major != o.major) {
            return Integer.compare(major, o.major);
        }
        if (minor != o.minor) {
            return Integer.compare(minor, o.minor);
        }
        return Integer.compare(patch, o.patch);
    }

    public String getKubernetesVersion() {
        return format("%d.%d.%d", major, minor, patch);
    }

    int getMajor() {
        return major;
    }

    int getMinor() {
        return minor;
    }

    int getPatch() {
        return patch;
    }
}
