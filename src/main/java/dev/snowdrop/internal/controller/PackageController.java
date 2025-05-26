package dev.snowdrop.internal.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.halkyon.pkg.crd.Package;
import io.halkyon.pkg.crd.PackageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageController {
    private final static Logger LOGGER = LoggerFactory.getLogger(PackageController.class);

    public static void runPackageController(KubernetesClient client) {
        try {
            SharedInformerFactory sharedInformerFactory = client.informers();
            SharedIndexInformer<Package> packageInformer = sharedInformerFactory.sharedIndexInformerFor(Package.class, 10 * 1000L);
            LOGGER.info("Informer factory initialized.");

            packageInformer.addEventHandler(
                new ResourceEventHandler<Package>() {
                    @Override
                    public void onAdd(Package pkg) {
                        PackageStatus status = new PackageStatus();
                        status.setMessage("package successfully installed");
                        pkg.setStatus(status);
                        client.resource(pkg).updateStatus();
                        LOGGER.info("{} package added and status updated", pkg.getMetadata().getName());
                    }

                    @Override
                    public void onUpdate(Package oldPkg, Package newPkg) {
                        LOGGER.info("{} package updated", oldPkg.getMetadata().getName());
                    }

                    @Override
                    public void onDelete(Package pkg, boolean b) {
                        LOGGER.info("{} package deleted", pkg.getMetadata().getName());
                    }
                });

            LOGGER.info("Starting all registered informers");
            sharedInformerFactory.startAllRegisteredInformers();

        } catch (Exception e) {
            LOGGER.error("Error initializing package controller", e);
        }
    }
}

