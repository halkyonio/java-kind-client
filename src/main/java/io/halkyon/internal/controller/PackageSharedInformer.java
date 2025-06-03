package io.halkyon.internal.controller;

import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.halkyon.platform.operator.crd.Package;
import io.halkyon.platform.operator.crd.PackageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageSharedInformer {
    private final static Logger LOGGER = LoggerFactory.getLogger(PackageSharedInformer.class);

    public static void runInformer(KubernetesClient client) {

        new Thread(() -> {
            try {
                SharedInformerFactory sharedInformerFactory = client.informers();
                SharedIndexInformer<Package> packageInformer = sharedInformerFactory.sharedIndexInformerFor(Package.class, 10 * 1000L);
                LOGGER.info("Informer factory initialized.");

                packageInformer.addEventHandler(
                    new ResourceEventHandler<Package>() {
                        @Override
                        public void onAdd(Package pkg) {
                            PackageStatus status = new PackageStatus();
                            status.setMessage("package successfully deployed");
                            pkg.setStatus(status);
                            client.resource(pkg).updateStatus();
                            LOGGER.info("status updated of the package name: {}", pkg.getMetadata().getName());

                            LOGGER.info("Building a pod ...");
                            Pod aPod = new PodBuilder()
                                //@formatter:off
                                .withNewMetadata()
                                  .withName("demo-pod1")
                                  .withNamespace("default")
                                  .addToOwnerReferences(new OwnerReferenceBuilder()
                                      .withName(pkg.getMetadata().getName())
                                      .withUid(pkg.getMetadata().getUid())
                                      .build())
                                .endMetadata()
                                .withNewSpec()
                                  .addNewContainer()
                                    .withName("nginx")
                                    .withImage("nginx:1.7.9")
                                    .addNewPort()
                                      .withContainerPort(80)
                                    .endPort()
                                  .endContainer()
                                .endSpec()
                                .build();
                                 //@formatter:on
                            LOGGER.info("Pod built ...");
                            client.resource(aPod).inNamespace("default").create();
                            LOGGER.info("Created a simple pod running: nginx");
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
        }, "controller-thread").start();
    }
}

