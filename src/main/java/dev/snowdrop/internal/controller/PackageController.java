package dev.snowdrop.internal.controller;

import dev.snowdrop.internal.crd.Package;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageController {
    private final static Logger LOGGER = LoggerFactory.getLogger(PackageController.class);

    public static void initPackageController(KubernetesClient client) {
        try {
            SharedInformerFactory sharedInformerFactory = client.informers();
            SharedIndexInformer<Package> packageInformer = sharedInformerFactory.sharedIndexInformerFor(Package.class, 30 * 1000L);
            LOGGER.info("Informer factory initialized.");

            packageInformer.addEventHandler(
                new ResourceEventHandler<Package>() {
                    @Override
                    public void onAdd(Package pkg) {
                        LOGGER.info("{} package added", pkg.getMetadata().getName());
                    }

                    @Override
                    public void onUpdate(Package pkg, Package pkg1) {
                        LOGGER.info("{} package updated", pkg.getMetadata().getName());
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

    public static void stopPackageController(KubernetesClient client) {
        try {
            SharedInformerFactory sharedInformerFactory = client.informers();
            SharedIndexInformer<Package> packageInformer = sharedInformerFactory.getExistingSharedIndexInformer(Package.class);

            if (packageInformer != null) {
                packageInformer.stop();
                sharedInformerFactory.stopAllRegisteredInformers();
            } else {
                LOGGER.warn("No shared informer found for: {}", Package.class.getSimpleName());
            }
        } catch (Exception e) {
            LOGGER.error("Error stopping package controller", e);
        }
    }
}

