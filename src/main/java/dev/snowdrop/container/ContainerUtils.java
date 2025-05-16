package dev.snowdrop.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class ContainerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerUtils.class);
    private static DockerClient dc;

    public static DockerClient ConfigureDockerClient() {
        if (dc == null) {
            try {
                var config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .build();
                var httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .build();
                dc = DockerClientImpl.getInstance(config, httpClient);
                return dc;
            } catch (Exception e) {
                LOGGER.error("Failed to create Docker client: {}", e.getMessage(), e);
                return null;
            }
        } else {
            return dc;
        }
    }

    public static String fetchContainerId(String containerName) throws IOException {

        List<Container> containers = dc.listContainersCmd()
            .withShowAll(true)
            .exec();

        for (Container container : containers) {
            for (String name : container.getNames()) {
                // container names are prefixed with '/'
                if (name.equals("/" + containerName)) {
                    LOGGER.info("Container ID: " + container.getId());
                    return container.getId();
                }
            }
        }
        return containerName;
    }

    public static String getContainerIPAddress(String containerId) {
        InspectContainerResponse icr = dc.inspectContainerCmd(containerId).exec();
        NetworkSettings ns = icr.getNetworkSettings();
        return ns.getNetworks().get("podman").getIpAddress();
    }
}
