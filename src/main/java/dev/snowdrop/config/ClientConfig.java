package dev.snowdrop.config;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;

@ConfigMapping(prefix = "kind")
public interface ClientConfig {
    /**
     * Name of the cluster
     */
    @WithDefault("kind")
    String name();

    /**
     * Labels to be added to the Node
     */
    String labels();

    /**
     * Binding configuration
     * It represents the ports to be bind between the container and the host
     */
     List<Binding> binding();

     interface Binding {
         /**
          * hostPort
          */
         String hostPort();

         /**
          * containerPort
          */
         String containerPort();
     }
}
