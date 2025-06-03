package io.halkyon.container;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

public class ContainerUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerUtils.class);
    private static DockerClient dc;
    private static final int CONTAINER_IP_TIMEOUT_MSECS = 60000;
    private static String LOCALHOST = "127.0.0.1";

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

    public static String getInternalIpAddress(InspectContainerResponse containerInfo) {
        return waitUntilNotNull(() -> {
                final Map<String, ContainerNetwork> networks = containerInfo.getNetworkSettings().getNetworks();
                if (networks.isEmpty()) {
                    return null;
                }
                return networks.entrySet().iterator().next().getValue().getIpAddress();
            },
            CONTAINER_IP_TIMEOUT_MSECS,
            "Waiting for network to receive internal IP address...",
            () -> new IllegalStateException("Failed to determine internal IP address")
        );
    }

    public static int getFreePortOnHost() throws IOException {
        InetAddress addr = InetAddress.getByName(LOCALHOST);
        ServerSocket socket = new ServerSocket(0, 0, addr);
        return socket.getLocalPort();
    }

    private static <T, E extends Exception> T waitUntilNotNull(
        final Supplier<T> check,
        final long timeout,
        final String message,
        final Supplier<E> error
    ) throws E {
        boolean first = true;
        final long start = currentTimeMillis();
        while ((currentTimeMillis() - start) < timeout) {
            final T result = check.get();
            if (result != null) {
                return result;
            }
            if (first) {
                LOGGER.info("{}", message);
            }
            first = false;
            try {
                sleep(100);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
        throw error.get();
    }
}
