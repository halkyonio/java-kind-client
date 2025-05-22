package dev.snowdrop.internal.crd;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.*;

@Group("halkyon.io")
@Version("v1alpha1")
@Kind("Package")
@Plural("packages")
@Singular("package")
public class Package extends CustomResource<PackageSpec, PackageStatus> {
    @Override
    public String toString() {
        return "Package{" +
            "apiVersion='" + getApiVersion() + '\'' +
            ", kind='" + getKind() + '\'' +
            ", metadata=" + getMetadata() +
            ", spec=" + getSpec() +
            ", status=" + getStatus() +
            '}';
    }
}
