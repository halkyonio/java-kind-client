package dev.snowdrop;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Supplier;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

public class KindContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KindContainer.class);
    private static final int CONTAINER_IP_TIMEOUT_MSECS = 60000;

    public static String getInternalIpAddress(InspectContainerResponse containerInfo) {
        return waitUntilNotNull(() -> {
                final Map<String, ContainerNetwork> networks = containerInfo.getNetworkSettings().getNetworks();
                if (networks.isEmpty()) {
                    return null;
                }
                ContainerNetwork cn = networks.get("podman");
                if (cn != null && cn.getIpAddress() != "") {
                    return cn.getIpAddress();
                } else {
                    return null;
                }
            },
            CONTAINER_IP_TIMEOUT_MSECS,
            "Waiting for network to receive internal IP address...",
            () -> new IllegalStateException("Failed to determine internal IP address")
        );
    }

    public static <T, E extends Exception> T waitUntilNotNull(
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
