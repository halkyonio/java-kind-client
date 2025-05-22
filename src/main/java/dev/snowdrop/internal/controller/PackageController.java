package dev.snowdrop.internal.controller;

import dev.snowdrop.internal.crd.Package;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageController {
    private final static Logger LOGGER = LoggerFactory.getLogger(PackageController.class);

    public static void initPackageController(KubernetesClient client) {
        try {
            MixedOperation<Package, KubernetesResourceList<Package>, Resource<Package>> packageOp = client.resources(Package.class);
            SharedIndexInformer<Package> bookSharedIndexInformer = packageOp.inNamespace("default").inform(new ResourceEventHandler<>() {
                @Override
                public void onAdd(Package pkg) {
                    LOGGER.info("{} ADDED");
                }

                @Override
                public void onUpdate(Package pkg, Package pkg1) {
                    LOGGER.info("{} UPDATED");
                }

                @Override
                public void onDelete(Package pkg, boolean b) {
                    LOGGER.info("{} DELETED");
                }
            });

            Thread.sleep(30 * 1000L);
            LOGGER.info("Package SharedIndexInformer open for 30 seconds");

            bookSharedIndexInformer.close();
        } catch (
            InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}

