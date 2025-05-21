package dev.snowdrop.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

@ConfigMapping(prefix = "kind")
public interface ClientConfig {
    /**
     * Version of kubernetes to be installed
     */
    @WithName("kubernetesVersion")
    Optional<String> kubernetesVersion();

    /**
     * Name of the cluster
     */
    @WithDefault("kind")
    String name();

    /**
     * Labels to be added to the Node
     */
    Optional<String> labels();

    /**
     * Binding configuration
     * It represents the ports to be bind between the container and the host
     */
    @WithName("bindings")
     List<Binding> binding();

     interface Binding {
         /**
          * hostPort
          */
         @WithName("hostPort")
         String hostPort();

         /**
          * containerPort
          */
         @WithName("containerPort")
         String containerPort();
     }
}
