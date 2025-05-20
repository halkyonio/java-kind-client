package dev.snowdrop.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "kind")
public interface ClientConfig {
    /**
     * Name of the cluster
     */
    String name();

    /**
     * Labels to be added to the Node
     */
    String labels();

    /**
     * Binding configuration
     * It represents the ports to be bind between the container and the host
     */
     Binding binding();

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
